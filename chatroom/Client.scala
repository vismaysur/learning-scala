package chatroom

import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.net.{Socket}
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.{Runtime, Thread}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.compiletime.uninitialized
import io.StdIn._

object Client {
  var isRunning = new AtomicBoolean(true)
  var in: BufferedReader = uninitialized
  var out: PrintStream = uninitialized
  var client: Socket = uninitialized

  def main(args: Array[String]): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread(() => cleanup()))

    client = new Socket("localhost", 4000)
    in = new BufferedReader(new InputStreamReader(client.getInputStream))
    out = new PrintStream(client.getOutputStream)

    Future {
      while (isRunning.get()) {
        if (in.ready()) {
          var msg = in.readLine
          if (msg == null) {
            cleanup()
            println("Server disconnected")
            System.exit(0)
          }
          println(msg)
        }
        Thread.sleep(100)
      }
    }

    while (true) {
      var msg = readLine
      if (msg == null || msg == ":quit") {
        cleanup()
        System.exit(0)
      }
      out.println(msg)
    }
  }

  def cleanup(): Unit = {
    isRunning.set(false)
    if (in != null) in.close()
    if (out != null) out.close()
    if (client != null && !client.isClosed) client.close()
  }
}
