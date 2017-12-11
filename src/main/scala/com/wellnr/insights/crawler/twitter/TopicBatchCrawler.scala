package com.wellnr.insights.crawler.twitter

import java.text.SimpleDateFormat

import akka.actor.Actor
import TopicBatchCrawler._
import com.wellnr.insights.config
import com.wellnr.insights.crawler.twitter.Model.Tweet

object Client {

  import twitter4j.conf.ConfigurationBuilder
  import twitter4j.{Query, TwitterFactory}

  import scala.collection.JavaConverters._

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private val cb = new ConfigurationBuilder()
    .setOAuthConsumerKey(config.app.crawler.twitter.consumerKey)
    .setOAuthConsumerSecret(config.app.crawler.twitter.consumerSecret)
    .setOAuthAccessToken(config.app.crawler.twitter.accessToken)
    .setOAuthAccessTokenSecret(config.app.crawler.twitter.accessTokenSecret)

  private val twitter = new TwitterFactory(cb.build()).getInstance()

  def query(q: String, batchSize: Int, maxId: Option[Long]): List[Tweet] = {
    val query = new Query(q)
    query.setCount(batchSize)

    maxId match {
      case Some(maxIdValue) => query.setMaxId(maxIdValue)
      case _ => // do nothing
    }

    twitter
      .search(query)
      .getTweets
      .asScala
      .reverse
      .map(status => Tweet(status.getId, status.getUser.getName, status.getText, dateFormatter.format(status.getCreatedAt)))
      .toList
  }
}

object TopicBatchCrawler {

  object Messages {
    sealed trait Message
    case class CrawlBatch(topic: String, maxId: Option[Long], batchSize: Int = 100) extends Message
    case class CrawlBatchResult(tweets: List[Tweet]) extends Message
  }

}

class TopicBatchCrawler extends Actor {

  def receive = {
    case Messages.CrawlBatch(topic, maxId, batchSize) =>
      sender ! Messages.CrawlBatchResult(Client.query(topic, batchSize, maxId))
  }

}
