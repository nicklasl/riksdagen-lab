package nu.nldv.riksdag

import akka.actor.{Props, Actor}
import akka.pattern.ask
import nu.nldv.riksdag.model.{Result, IntressentPair, Intressent, DownloadTask}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.mutable
import play.api.http.HeaderNames
import play.api.libs.json.{JsArray, JsValue}
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.ning.NingWSClient

class DownloadActor(client: NingWSClient) extends Actor {

  val fileWriterActor = context.actorOf(Props[FileWritingActor], name = s"nu.nldv.riksdag.FileWritingActor-${this.hashCode()}")
  def uri(baseUri: String ,page: Int) = s"$baseUri&p=$page"

  def receive: Actor.Receive = {
    case task: DownloadTask => {
      val mainSender = sender()
      val pagesToFetch = {
        val result: Int = Await.result(client.url(task.baseUri).get().map {
          response =>
            (response.json \ "dokumentlista" \ "@sidor").as[String].toInt
        }, Duration("10 seconds"))
        println(s"Will fetch $result pages")
        result
      }
      val vertices = new mutable.HashSet[Intressent]
      val edges = new mutable.HashMap[IntressentPair, Int]
      var totalBytesTransferred = 0

      def partioverskridandeSamarbete(intressent1: Intressent, intressent2: Intressent): Boolean = intressent1.partibet != intressent2.partibet

      println(s"Starting fetching with uri=${uri(task.baseUri, 1)}")

      val seq = for (page <- Range(1, pagesToFetch + 1)) yield {
        client.url(uri(task.baseUri, page)).get().map {
          response =>
            val contentLength: Option[String] = response.header(HeaderNames.CONTENT_LENGTH)
            totalBytesTransferred+=contentLength.map(i => i.toInt).getOrElse(0)
            val docs: Seq[JsValue] = ((response.json \ "dokumentlista") \ "dokument").as[JsArray].value
            docs.map {
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
                          if (!intressent.equals(intressent2) && (task.innerPartyCooperation || partioverskridandeSamarbete(intressent, intressent2))) {
                            val key: IntressentPair = IntressentPair(intressent, intressent2)
                            val hitOption = edges.get(key)
                            if (hitOption.isDefined) edges.update(key, hitOption.get + 1)
                            else edges.put(key, 1)
                          }
                      }

                  }
                }
            }
        }
      }
      println(s"waiting for ${seq.size} request(s) to finish")
      seq.foreach(fut => Await.ready(fut, Duration(30, TimeUnit.SECONDS)))
      println(s"Finished all requests. Total transferred = ${totalBytesTransferred / 1000} KB, ${totalBytesTransferred / 1000000} MB")

      val result = Result(task.year.toString, vertices.toSet, edges.toMap)
      implicit val timeout = Timeout(5, TimeUnit.SECONDS)
      fileWriterActor ? result map {
        case "done" => {
          println(s"Done writing file for year=${task.year}")
          mainSender ! "done"
        }
        case _ => throw new Exception("Got nothing back from Filewriter")
      }
    }
  }
}

object DownloadActor {
  def props(client: NingWSClient) = Props(new DownloadActor(client))
}

