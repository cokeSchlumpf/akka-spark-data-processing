package com.wellnr.insights.crawler.twitter

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, FSM, Props}
import Crawler._
import com.wellnr.insights.crawler.twitter.Model._

object Crawler {

  /*
   * Messages
   */
  sealed trait Messages

  case object Crawl extends Messages

  case class CrawledTweetsRequest(topic: String) extends Messages

  case class CrawledTweetsResponse(topic: String, tweets: List[Tweet]) extends Messages

  /*
   * State
   */
  sealed trait State

  case object Available extends State

  /*
   * Data
   */
  case class TopicData(actor: ActorRef, tweets: List[Tweet])

  case class Data(topics: Map[String, TopicData] = Map())

}

trait Crawler$Helper extends Actor with ActorLogging {

  def dataWithCrawlingResult(data: Data, topic: String, tweets: List[Tweet]) = {
    log.info(s"received tweets from child crawler (size: ${tweets.size})")

    val existingTopicData = data.topics(topic)
    val updatedTopicData = existingTopicData.copy(tweets = existingTopicData.tweets ++ tweets)

    val existingTopics = data.topics
    val updatedTopics = existingTopics + (topic -> updatedTopicData)

    data.copy(updatedTopics)
  }

  def initializeTopicData(topic: String) = {
    val actor = context.actorOf(Props(new TopicCrawler(topic)))
    TopicData(actor, List())
  }

  def startCrawling(data: Data) = {
    data.topics.foreach {
      case (_, topicData) =>
        topicData.actor ! TopicCrawler.StartCrawling
    }
  }

}

/*
 * Actor
 */
case class Crawler(topics: List[String]) extends FSM[State, Data] with Crawler$Helper {

  startWith(Available, Data(
    topics.map(topic => topic -> initializeTopicData(topic)).toMap
  ))

  when(Available) {
    case Event(Crawl, data: Data) =>
      startCrawling(data)
      stay()

    case Event(TopicCrawler.CrawlingResult(topic, tweets), data: Data) =>
      stay() using dataWithCrawlingResult(data, topic, tweets)

    case Event(TopicCrawler.CrawlingDone(topic), _) =>
      log.info(s"Crawling session done for topic ${topic}")
      stay()

    case Event(CrawledTweetsRequest(topic), data: Data) =>
      sender ! CrawledTweetsResponse(topic, data.topics(topic).tweets)
      stay()
  }

  initialize()

}


