import play.api.libs.json.{Json, Reads}

/**
 * Created with IntelliJ IDEA.
 * User: nicklas
 * Date: 2014-10-22
 * Time: 20:27
 */
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


case class IntressentPair(intressent1: Intressent, intressent2: Intressent){

  override def hashCode(): Int = intressent1.hashCode() + intressent2.hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case intressentPair: IntressentPair => {
      intressentPair.intressent1.intressent_id.equalsIgnoreCase(this.intressent1.intressent_id) ||
        intressentPair.intressent1.intressent_id.equalsIgnoreCase(this.intressent2.intressent_id) ||
        this.intressent1.intressent_id.equalsIgnoreCase(intressentPair.intressent2.intressent_id) ||
        this.intressent2.intressent_id.equalsIgnoreCase(intressentPair.intressent1.intressent_id)
    }
    case _ => false
  }

  override def toString: String = s"i1=${intressent1.intressent_id}\ti2=${intressent2.intressent_id}"
}
