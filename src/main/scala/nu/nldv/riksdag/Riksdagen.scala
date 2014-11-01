package nu.nldv.riksdag

import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import nu.nldv.riksdag.model.DownloadTask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import play.api.libs.ws.ning.NingWSClient
import com.ning.http.client.AsyncHttpClientConfig
import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration.Duration


object Riksdagen extends App {
  val actorSystem = akka.actor.ActorSystem("ActorSystem")

  def loadArgs: Option[Int] = if (args.length > 0) Some(args(0).toInt) else None
  implicit val timeout = Timeout(2, TimeUnit.MINUTES)

  val builder = new AsyncHttpClientConfig.Builder()
  val client = new NingWSClient(builder.build())

  private val futures: IndexedSeq[Future[Unit]] = for (year <- Range(1990, 2015)) yield {
    val mappaSamarbeteInomPartier = false
    val baseUri = s"http://data.riksdagen.se/dokumentlista/?sok=&doktyp=mot&rm=&from=$year-01-01&tom=$year-12-31&sort=rel&sortorder=desc&utformat=json"

    val downloadActor = actorSystem.actorOf(DownloadActor.props(client), name = s"nu.nldv.riksdag.DownloadActor-$year")

    val future: Future[Unit] = downloadActor ? DownloadTask(baseUri, mappaSamarbeteInomPartier, year) map {
      case "done" => {
        println(s"Done downloading and writing file for year=$year")
      }
      case _ => throw new Exception("Got nothing back from DownloadActor")
    }
    Await.ready(future, timeout.duration)
  }

  futures.foreach(fut => Await.ready(fut, Duration(2, TimeUnit.MINUTES)))

  println("done done. closing stuff.")
  client.close()
  actorSystem.shutdown()

}
