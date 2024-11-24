package chatroom

import java.net.{Socket, ServerSocket, InetSocketAddress}
import java.io.{InputStreamReader, BufferedReader, PrintStream}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.{Runtime, Thread} 
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

object Server {
  case class User(name: String, socket: Socket, in: BufferedReader, out: PrintStream)

  var users = new ConcurrentHashMap[String, User]().asScala 
  var isRunning = new AtomicBoolean(true)

  def main(args: Array[String]): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run(): Unit = {
        isRunning.set(false)
      }
    }))

    Future {
      runServer()
    }
    
    println("Server listening on port 4000")

    while (true) {
      for ((name: String, user: User) <- users) {
        if (user.in.ready()) {
          var msg = user.in.readLine
          if (msg == ":quit") {
            users -= name
          } else {
            broadcast(user.name + ": " + msg)
          }
        }
      }
    }
  }

  def runServer(): Unit = {
    var server = new ServerSocket
    server.bind(new InetSocketAddress("localhost", 4000))

    while (isRunning.get()) {
      var clientSocket = server.accept
      var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
      var out = new PrintStream(clientSocket.getOutputStream)

      out.println("Please enter your name: ")
      var name = in.readLine
        
      if (name != null && !users.contains(name)) {
        var newUser = new User(name, clientSocket, in, out)
        users += (name -> newUser)
        out.println("Server: Hey " + name + ". Welcome to The Chatroom")
      } else {
        out.println("Invalid name / name already taken")
        clientSocket.close()
      }
    }

    server.close()
  }

  def broadcast(msg: String): Unit = {
    for ((_: String, user: User) <- users) {
      user.out.println(msg)
    }
  }

  def cleanup(): Unit = {
    isRunning.set(false)
  }
}
