import play.api.libs.json.{JsArray, JsValue}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object Riksdagen extends App {

  val baseUri = "http://data.riksdagen.se/dokumentlista/?sok=&doktyp=mot&sort=datum&sortorder=desc&utformat=json&a=s"

  def uri(page: Int) = s"$baseUri&p=$page"

  def loadArgs: Option[Int] = if (args.length > 0) Some(args(0).toInt) else None

  val pagesToFetch = loadArgs.getOrElse({
    println("No arguments supplied. Will only fetch 1 page.")
    1
  })

  val vertices = new mutable.HashMap[String, String]
  val edges = new mutable.HashMap[(String, String), Int]

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  val seq = for (page <- Range(1, pagesToFetch + 1)) yield {
    client.url(uri(page)).get().map {
      response =>
        val docs: Seq[JsValue] = ((response.json \ "dokumentlista") \ "dokument").as[JsArray].value
        val list: List[String] = docs.map {
          json =>
            val intressenter = {
              val value: JsValue = json \ "dokintressent" \ "intressent"
              if (value.isInstanceOf[JsArray]) value.as[JsArray].value
              else Seq(value)
            }
            if (intressenter.size > 1) {
              intressenter.foreach {
                intressent =>
                  def intressentId(intressent: JsValue): String = (intressent \ "intressent_id").as[String]
                  vertices.put(intressentId(intressent), (intressent \ "namn").as[String])
                  intressenter.foreach {
                    intressent2 =>
                      val key: (String, String) = (intressentId(intressent), intressentId(intressent2))
                      val hitOption = edges.get(key)
                      if(hitOption.isDefined) edges.update(key, hitOption.get + 1)
                      else edges.put(key, 1)
                  }

              }
            }
            s"klar med $page"
        }.toList
        list.foreach(println(_))
    }
  }
  println(s"waiting for ${seq.size} request(s) to finish")
  seq.foreach(fut => Await.ready(fut, Duration("15 seconds")))

  println("VERTICES!")
  println(vertices)

  println("EDGES!")
  println(edges.filter(x => x._2 > 1))

  client.close()
}
