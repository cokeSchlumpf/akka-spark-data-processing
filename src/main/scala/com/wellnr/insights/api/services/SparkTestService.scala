package com.wellnr.insights.api.services

import akka.actor.{Actor, ActorSystem, Props}
import SparkActor._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.apache.spark.sql.SparkSession


object SparkActor {

  trait Messages

  case object Test extends Messages

}

case class SparkActor() extends Actor {

  def receive = {
    case Test =>
      val logFile = "/Users/michael/Downloads/spark-2.2.1-bin-hadoop2.7/README.md"
      val spark = SparkSession.builder
        .appName("Simple Application")
        .master("spark://localhost:7077")
        .config("spark.submit.deployMode","cluster")
        .getOrCreate()


      val logData = spark.read.textFile(logFile).cache()
      val numAs = logData.filter(line => line.contains("a")).count()
      val numBs = logData.filter(line => line.contains("b")).count()

      println(s"Lines with a: $numAs, Lines with b: $numBs")

      spark.stop()
  }

}

case class SparkTestService()(implicit system: ActorSystem) {

  object actors {
    val spark = system.actorOf(Props[SparkActor])
  }

  val route: Route = {
    get {
      actors.spark ! Test
      complete("Ok")
    }
  }

}
