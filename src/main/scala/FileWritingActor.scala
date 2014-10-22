import akka.actor.Actor
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

class FileWritingActor extends Actor {

  def receive: Actor.Receive = {
    case result: Result => {
      println(s"Received and will write Result for year ${result.year}")
      Files.write(Paths.get(s"${result.year}.net"), createString(result).getBytes(StandardCharsets.UTF_8))

    }
    case _ => println("Got nothing!")
  }

  def createString(result: Result): String = ???
}
