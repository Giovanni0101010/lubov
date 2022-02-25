package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main {
  val REFRESH_TIME = 10 * 60 * 1000

  val source = scala.io.Source.fromFile("urls.txt")
  var file = source.getLines().toList

  var lastUpdate = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    if (System.currentTimeMillis() - lastUpdate > REFRESH_TIME) {
      file = fetchUrls
      println(s"list updated. size=${file.size}")
    }
    val (queue, aggregationSource) = akka.stream.scaladsl.Source
      .queue[String](file.size, OverflowStrategy.backpressure)
      .throttle(50, 1.second)
      .wireTap(Flow.fromFunction[String, Future[HttpResponse]](url => {
        println(s"$url -- start with love")
        Http().singleRequest(HttpRequest(uri = url.replace(" ", "")))
      })
        .async
        .to(Sink
          .foreach(
            responseFuture => {
              responseFuture.onComplete { result => {
                result match {
                  case Success(value) => println(s"Response status: ${value.status}")
                  case Failure(exception) => println(s"Error happened: ${exception.getClass.getName}")
                }
              }
              }
            }
          )))
      .preMaterialize()

    Source(file)
      .via(
        Flow[String]
          .merge(aggregationSource)
          .map(p => queue.offer(p))
      )
      .run()
  }


  private def fetchUrls = {
    val source = scala.io.Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt")
    try {
      source.getLines.toList
    } finally {
      source.close()
    }
  }
}
