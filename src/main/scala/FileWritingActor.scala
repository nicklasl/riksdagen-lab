import akka.actor.Actor
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec

class FileWritingActor extends Actor {

  def receive: Actor.Receive = {
    case result: Result => {
      println(s"Writing file for year=${result.year}")
      implicit val numberForIntressent: Map[String, Int] = createRunningNumbers(result.vertices)
      Files.write(Paths.get(s"Network-${result.year}.net"), createString(result).getBytes(StandardCharsets.UTF_8))
      sender() ! "done"
    }
    case _ => println("Got nothing!")
  }

  def createRunningNumbers(set: Set[Intressent]): Map[String, Int] = {

    @tailrec
    def iter(set: Set[Intressent], i: Int, accumulator: Map[String, Int]): Map[String, Int] = {
      if (set.isEmpty) accumulator
      else {
        iter(set.tail, i + 1, accumulator.+((set.head.intressent_id, i)))
      }
    }
    iter(set, 1, Map[String, Int]())
  }

  def createString(result: Result)(implicit numberForIntressent: Map[String, Int]): String = {
    s"""*Vertices ${result.vertices.size}
      |${verticesOnOwnRow(result.vertices)}
      |*Edges
      |${edgesOnOwnRow(result.edges)}""".stripMargin

  }

  def partibeteckning(option: Option[String]): String = {
    if (option.isDefined) s"(${option.get})"
    else ""
  }

  def verticesOnOwnRow(set: Set[Intressent])(implicit numberForIntressent: Map[String, Int]): String =
    set.map(i => s"${numberForIntressent(i.intressent_id)} '${i.namn} ${partibeteckning(i.partibet)}'").mkString("\n")

  def edgesOnOwnRow(map: Map[IntressentPair, Int])(implicit numberForIntressent: Map[String, Int]) = map.map(entry =>
    s"${numberForIntressent(entry._1.intressent1.intressent_id)} ${numberForIntressent(entry._1.intressent2.intressent_id)} ${entry._2}"
  ).mkString("\n")
}
