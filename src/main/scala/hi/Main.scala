package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

import java.io.{BufferedWriter, File, FileWriter}
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

object Main {
  val REFRESH_TIME = 10 * 60 * 1000

  var file = List()

  var lastUpdate = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val ddosGuardUrls = mutable.HashSet[String]()

    val (queue, aggregationSource) = Source
      .queue[String](1, OverflowStrategy.backpressure)
      .throttle(20, 1.second)
      .preMaterialize()

    val updateList = Source(fetchUrls)
      .throttle(400, 5.minute)

    val redirection = Flow
      .fromFunction[String, (String, Future[HttpResponse])](f => {
        (f, Http().singleRequest(HttpRequest(uri = f.replace(" ", ""))))
      })
      .to(Sink
        .foreach(r => r._2.map({
          case HttpResponse(StatusCodes.Redirection(intValue), headers, _, _) =>
            println(s"redirect $intValue, $headers")
            val link = headers.filter(ss => ss.name().equals("Location"))
            if (link.nonEmpty && link.head.value().startsWith("http")) queue.offer(link.head.value())
          case HttpResponse(StatusCodes.OK, _, _, _) =>
            println(s"${r._1} is working")
          case HttpResponse(StatusCodes.ServerError(intValue), _, _, _) =>
            println(s"${r._1} is dead by $intValue")
          case HttpResponse(StatusCodes.Forbidden, _, entity, _) =>
            ddosGuardUrls.add(r._1)
            println(s"${r._1} DDOS-GUARD")
          case HttpResponse(status, _, entity, _) =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              toFile(status.intValue() + "-" + r._1, body.utf8String, "failures")

              println(s"${r._1} ${status.intValue()} unknown error")
            }
        })))
    Source(file)
      .via(
        Flow[String]
          .merge(aggregationSource)
          .merge(updateList)
          .wireTap(redirection)
          .map(p => queue.offer(p))
      )
      .run()
  }

    def toFile(name: String, content: String, path: String) = {

      val file = new File(path + "/" + name
        .replaceAll(":", "")
        .replaceAll("/", "")
        .replaceAll("\\?", "")
        .replaceAll("#", "") + ".html")
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(content)
      bw.close()
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