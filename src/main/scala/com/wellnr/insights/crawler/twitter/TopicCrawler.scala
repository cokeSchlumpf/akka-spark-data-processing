package com.wellnr.insights.crawler.twitter

import java.text.SimpleDateFormat

import akka.actor.{ActorRef, FSM, Props}
import com.wellnr.insights.crawler.twitter.Model.Tweet
import com.wellnr.insights.crawler.twitter.TopicCrawler._
import com.wellnr.insights.util.{FailingActor, RetryingActor}

import scala.concurrent.duration._

object TopicCrawler {

  /*
   * Messages
   */
  sealed trait InMessages
  case object StartCrawling extends InMessages


  sealed trait OutMessages
  case class CrawlingDone(topic: String) extends OutMessages
  case class CrawlingResult(topic: String, tweets: List[Tweet]) extends OutMessages

  /*
   * State
   */
  sealed trait State
  case object Available extends State
  case object WaitingForBatch extends State

  /*
   * Data
   */
  trait Data
  case class AvailableData(lastCrawledId: Option[Long]) extends Data
  case class WaitingForBatchData(lastCrawledId: Option[Long], sender: ActorRef, newLastCrawledId: Option[Long] = None) extends Data

}

case class TopicCrawler(topic: String, batchSize: Int = 100) extends FSM[State, Data] {

  def TopicBatchCrawlerActor = {
    val actual = context.actorOf(Props[TopicBatchCrawler].withDispatcher("dispatchers.blocking-dispatcher"))
    val failing = context.actorOf(Props(FailingActor(actual, 10.seconds)))
    val retryingWrapperActor = context.actorOf(Props(RetryingActor(5, 5.seconds, 10.seconds, failing)))
    retryingWrapperActor
  }

  startWith(Available, AvailableData(None))

  when(Available) {
    case Event(StartCrawling, AvailableData(lastCrawledId)) =>
      TopicBatchCrawlerActor ! TopicBatchCrawler.Messages.CrawlBatch(topic, None, batchSize)
      goto(WaitingForBatch) using WaitingForBatchData(lastCrawledId, sender)
  }

  when(WaitingForBatch) {
    case Event(TopicBatchCrawler.Messages.CrawlBatchResult(tweets), data: WaitingForBatchData) =>
      if (tweets.nonEmpty) {
        data.lastCrawledId match {
          case Some(lastCrawledId) =>
            if (lastCrawledId < tweets.head.id) {
              data.sender ! CrawlingResult(topic, tweets)
              TopicBatchCrawlerActor ! TopicBatchCrawler.Messages.CrawlBatch(topic, Some(tweets.head.id - 1), batchSize)
              stay() using data.copy(newLastCrawledId = Some(data.lastCrawledId.getOrElse(tweets.last.id)))
            } else {
              val onlyNewTweets = tweets.filter(tweet => lastCrawledId > tweet.id)
              data.sender ! CrawlingResult(topic, onlyNewTweets)
              data.sender ! CrawlingDone(topic)
              goto(Available) using AvailableData(data.newLastCrawledId.orElse(data.lastCrawledId))
            }
          case None =>
            data.sender ! CrawlingResult(topic, tweets)
            data.sender ! CrawlingDone(topic)
            goto(Available) using AvailableData(Some(tweets.last.id))
        }
      } else {
        data.sender ! CrawlingDone(topic)
        goto(Available) using AvailableData(data.newLastCrawledId.orElse(data.lastCrawledId))
      }

    case Event(StartCrawling, _) =>
      // ignore fow now
      stay()
  }

  initialize()

}
