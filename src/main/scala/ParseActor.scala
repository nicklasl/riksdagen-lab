import akka.actor.{Props, Actor}
import akka.event.Logging
import play.api.libs.json.{JsArray, JsValue}
import akka.pattern.ask

class ParseActor extends Actor {

  import ParseActor._

  val log = Logging(context.system, this)
  val writeActor = context.actorOf(Props[WriteActor], name = "WriteActor")


  def receive: Actor.Receive = {
    case DocsToParse(docs) => {
      log.debug("Got some docs to parse")
      println("Got some docs to parse")
      val list: List[String] = docs.map {
        json =>
          val intressenter = {
            val value: JsValue = json \ "dokintressent" \ "intressent"
            if (value.isInstanceOf[JsArray]) value.as[JsArray].value
            else Seq(value)
          }
          val intressenterString: Seq[String] = intressenter.map {
            intressent => {
              val namn = (intressent \ "namn").asOpt[String].getOrElse("inget namn")
              val parti = (intressent \ "partibet").asOpt[String].getOrElse("inget parti")
              s"namn: ${namn}, parti: ${parti}"
            }
          }
          val motionsNamn = (json \ "titel").asOpt[String].getOrElse((json \ "id").as[String])
          s"${motionsNamn} [${intressenterString.mkString(", ")}]"
      }.toList
      writeActor ! StringsToWrite(list)
    }
  }
}

class WriteActor extends Actor {
  import ParseActor._
  val log = Logging(context.system, this)

  def receive: Actor.Receive = {
    case StringsToWrite(list) => {
      log.debug("Got some strings to write")
      println("Got some strings to write")

      list.foreach(println(_))

    }
  }
}

object ParseActor {

  case class DocsToParse(docs: Seq[JsValue])

  case class StringsToWrite(strings: Seq[String])

}
