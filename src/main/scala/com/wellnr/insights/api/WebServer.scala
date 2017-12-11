package com.wellnr.insights.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.wellnr.insights.api.services.{AboutService, SparkTestService, TwitterService}
import com.wellnr.insights.config

/**
  * Created by michael on 08/12/2017.
  */
object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("wellnr-insights")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    object services {
      val about = AboutService(config.app.name, config.app.version)
      val spark = SparkTestService()
      val twitter = TwitterService()
      val foo = "foo"
    }

    // Access services to initialize
    services.foo

    val route =
      pathPrefix("api" / "v1") {
        pathPrefix("about") {
          services.about.route
        } ~
        pathPrefix("twitter") {
          services.twitter.route
        } ~
        pathPrefix("spark") {
          services.spark.route
        }
      }

    Http().bindAndHandle(route, "localhost", 8080)
    println("Serving API via localhost:8080")
  }

}
