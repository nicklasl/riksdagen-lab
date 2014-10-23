import akka.actor.Actor
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

class FileWritingActor extends Actor {

  def receive: Actor.Receive = {
    case result: Result => {
      println(s"Writing file for year=${result.year}")
      Files.write(Paths.get(s"Network-${result.year}.net"), createString(result).getBytes(StandardCharsets.UTF_8))
      sender() ! "done"
    }
    case _ => println("Got nothing!")
  }



  def createString(result: Result): String = {
    s"""*Vertices ${result.vertices.size}
      |${verticesOnOwnRow(result.vertices)}
      |*Edges
      |${edgesOnOwnRow(result.edges)}""".stripMargin

  }

  def partibeteckning(option: Option[String]): String = {
    if(option.isDefined) s"(${option.get})"
    else ""
  }

  def verticesOnOwnRow(set: Set[Intressent]): String = set.map(i => s"${i.intressent_id} '${i.namn} ${partibeteckning(i.partibet)}'").mkString("\n")

  def edgesOnOwnRow(map: Map[IntressentPair, Int]) = map.map(entry =>
    s"${entry._1.intressent1.intressent_id} ${entry._1.intressent2.intressent_id} ${entry._2}"
  ).mkString("\n")
}
