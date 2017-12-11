package com.wellnr.insights.api.services

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.wellnr.insights.crawler.twitter
import com.wellnr.insights.crawler.twitter.Model.Tweet
import spray.json
import spray.json.{DefaultJsonProtocol, PrettyPrinter}

import scala.concurrent.duration._

case class TwitterService()(implicit system: ActorSystem)  extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val printer: json.PrettyPrinter.type = PrettyPrinter

  implicit val itemFormat = jsonFormat4(Tweet)

  implicit val timeout = Timeout(20.seconds)

  val topics = List("@realDonaldTrump")

  object actors {
    object twitter {
      import com.wellnr.insights.crawler.twitter._

      val crawler = system.actorOf(Props(Crawler(topics)))
    }
  }

  val scheduler = QuartzSchedulerExtension(system)
  scheduler.schedule("twitter-crawler", actors.twitter.crawler, twitter.Crawler.Crawl)

  val route: Route =
    pathPrefix("crawl") {
      get {
        actors.twitter.crawler ! twitter.Crawler.Crawl
        complete(StatusCodes.OK, "ok")
      }
    } ~
    pathPrefix("tweets") {
      get {
        onSuccess(actors.twitter.crawler ? twitter.Crawler.CrawledTweetsRequest(topics.head)) {
          case twitter.Crawler.CrawledTweetsResponse(_, tweets) =>
            complete(tweets)
          case _ =>
            complete(StatusCodes.InternalServerError)
        }
      }
    }
}
