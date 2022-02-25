package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source

object Main {
  //val file = Source.fromFile("urls.txt").getLines.toList
  val file = Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt").getLines.toList


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    var i = 0
    while (true) {
      println(s"iteration $i")

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
}
