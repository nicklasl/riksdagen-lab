import play.api.libs.json.{JsArray, JsValue}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object Riksdagen extends App {


  def uri(page: Int) = s"$baseUri&p=$page"

  def loadArgs: Option[Int] = if (args.length > 0) Some(args(0).toInt) else None

  val year = loadArgs.getOrElse(2014)
  val baseUri = s"http://data.riksdagen.se/dokumentlista/?sok=&doktyp=mot&rm=&from=$year-01-01&tom=$year-12-31&sort=rel&sortorder=desc&utformat=json"

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  val pagesToFetch = {
    println("No arguments supplied. Will look up how many pages to fetch")
    val result: Int = Await.result(client.url(baseUri).get().map {
      response =>
        (response.json \ "dokumentlista" \ "@sidor").as[String].toInt
    }, Duration("10 seconds"))
    println(s"Will fetch $result pages")
    result
  }
  val vertices = new mutable.HashSet[Intressent]
  val edges = new mutable.HashMap[IntressentPair, Int]


  def partioverskridandeSamarbete(intressent1: Intressent, intressent2: Intressent): Boolean = intressent1.partibet != intressent2.partibet

  val seq = for (page <- Range(1, pagesToFetch + 1)) yield {
    client.url(uri(page)).get().map {
      response =>
        val docs: Seq[JsValue] = ((response.json \ "dokumentlista") \ "dokument").as[JsArray].value
        val list: List[String] = docs.map {
          json =>
            val intressenter = {
              val value: JsValue = json \ "dokintressent" \ "intressent"
              if (value.isInstanceOf[JsArray]) value.as[JsArray].value.map(js => js.as[Intressent])
              else Seq(value.as[Intressent])
            }
            if (intressenter.size > 1) {
              intressenter.foreach {
                intressent =>
                  vertices.add(intressent)
                  intressenter.foreach {
                    intressent2 =>
                      if (!intressent.equals(intressent2) && partioverskridandeSamarbete(intressent, intressent2)) {
                        val key: IntressentPair = IntressentPair(intressent, intressent2)
                        val hitOption = edges.get(key)
                        if (hitOption.isDefined) edges.update(key, hitOption.get + 1)
                        else edges.put(key, 1)
                      }
                  }

              }
            }
            s"klar med $page"
        }.toList
    }
  }
  println(s"waiting for ${seq.size} request(s) to finish")
  seq.foreach(fut => Await.ready(fut, Duration("15 seconds")))

  println(s"VERTICES: ${vertices.size}")
  println(vertices)

  println(s"EDGES: ${edges.size}")
  edges.foreach(println(_))

  client.close()
}
