import akka.actor.Props
import play.api.libs.json.{JsArray, JsValue}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import ParseActor._

object Riksdagen extends App {

  val actorSystem = akka.actor.ActorSystem("ActorSystem")
  val parseActor = actorSystem.actorOf(Props[ParseActor], name = "ParseActor")

  val baseUri = "http://data.riksdagen.se/dokumentlista/?sok=&doktyp=mot&sort=datum&sortorder=desc&utformat=json&a=s"

  def uri(page: Int) = s"$baseUri&p=$page"

  def loadArgs: Option[Int] = if (args.length > 0) Some(args(0).toInt) else None

  val pagesToFetch = loadArgs.getOrElse({
    println("No arguments supplied. Will only fetch 1 page.")
    1
  })

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  val seq = for (page <- Range(1, pagesToFetch + 1)) yield {
    client.url(uri(page)).get().map {
      response =>
        val docs: Seq[JsValue] = ((response.json \ "dokumentlista") \ "dokument").as[JsArray].value
        parseActor ! DocsToParse(docs)
    }
  }
  seq.foreach(fut => Await.ready(fut, Duration("5 seconds")))

  println(s"waiting for ${seq.size} request(s) to finish")
  actorSystem.stop(parseActor)
  client.close()

}
