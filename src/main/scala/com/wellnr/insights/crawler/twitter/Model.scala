package com.wellnr.insights.crawler.twitter

import java.util.Date

/**
  * Created by michael on 09/12/2017.
  */
object Model {

  final case class Tweet(id: Long, author: String, text: String, date: String)

}
