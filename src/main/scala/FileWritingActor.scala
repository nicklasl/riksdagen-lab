import akka.actor.Actor
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

class FileWritingActor extends Actor {

  def receive: Actor.Receive = {
    case result: Result => {
      println(s"Received and will write Result for year ${result.year}")
      Files.write(Paths.get(s"Network-${result.year}.net"), createString(result).getBytes(StandardCharsets.UTF_8))
      println(s"Should generate file named= Network-${result.year}.net")
      println(createString(result))
    }
    case _ => println("Got nothing!")
  }



  def createString(result: Result): String = {
    s"""*Vertices ${result.vertices.size}
      |${verticesOnOwnRow(result.vertices)}
      |*Edges
      |${edgesOnOwnRow(result.edges)}""".stripMargin

  }

  def verticesOnOwnRow(set: Set[Intressent]): String = set.map(i => s"${i.intressent_id} '${i.namn}'").mkString("\n")
  def edgesOnOwnRow(map: Map[IntressentPair, Int]) = map.map(entry =>
    s"${entry._1.intressent1.intressent_id} ${entry._1.intressent2.intressent_id} ${entry._2}"
  ).mkString("\n")
}
