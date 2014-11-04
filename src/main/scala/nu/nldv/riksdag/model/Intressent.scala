package nu.nldv.riksdag.model

import play.api.libs.json.Json

case class Intressent(intressent_id: String, namn: String, partibet: Option[String]){
  override def hashCode(): Int = this.intressent_id.hashCode

  override def equals(obj: scala.Any): Boolean = obj match {
    case intressent:Intressent => intressent.intressent_id.equalsIgnoreCase(this.intressent_id)
    case _ => false
  }

}

object Intressent {
  implicit val reads = Json.reads[Intressent]
}
