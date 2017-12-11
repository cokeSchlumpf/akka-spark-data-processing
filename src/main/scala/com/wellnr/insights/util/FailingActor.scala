package com.wellnr.insights.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import scala.concurrent.duration._

import scala.util.{Failure, Success}

/**
  * Created by michael on 10/12/2017.
  */
case class FailingActor(forwardTo: ActorRef, timeout: FiniteDuration) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  val rnd = new scala.util.Random

  def receive = {
    case message @ _ =>
      if ((1 + rnd.nextInt(4)) == 1) {
        log.info(s"Simulating a failure of ${forwardTo} ...")
      } else {
        val originalSender = sender
        (forwardTo ? message) (timeout) onComplete {
          case Success(result) =>
            originalSender ! result
          case Failure(_) =>
            log.warning(s"There was an actual failure calling ${forwardTo}")
        }
      }
  }

}
