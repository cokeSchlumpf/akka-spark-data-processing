package com.wellnr

import com.typesafe.config.ConfigFactory

/**
  * Created by michael on 08/12/2017.
  */
package object insights {

  val c = ConfigFactory.load()

  object config {
    object app {
      val name = c.getString("app.name")
      val version = c.getString("app.version")
      object crawler {
        object twitter {
          val accessToken = c.getString("app.crawler.twitter.accessToken")
          val accessTokenSecret = c.getString("app.crawler.twitter.accessTokenSecret")
          val consumerKey = c.getString("app.crawler.twitter.consumerKey")
          val consumerSecret = c.getString("app.crawler.twitter.consumerSecret")
        }
      }
    }
  }

}
