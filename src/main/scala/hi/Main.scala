package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source

object Main {
  val REFRESH_TIME = 10 * 60 * 1000

  var file = fetchUrls

  var lastUpdate = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    var i = 0
    while (true) {
      println(s"iteration $i")

      if (System.currentTimeMillis() - lastUpdate > REFRESH_TIME) {
        file = fetchUrls
        println(s"list updated. size=${file.size}")
      }

      file.foreach {
        url =>
          println(s"$url -- start with love")
          val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = url))

          responseFuture.map {
            case response@HttpResponse(StatusCodes.OK, _, _, _) =>
              println("response OK")
            case err =>
              sys.error(s"server is not responding: $err")
          }
      }

      Thread.sleep(50)
      i += 1
    }
  }

  private def fetchUrls = {
    val source = Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt")
    try {
      source.getLines.toList
    } finally {
      source.close()
    }
  }
}
