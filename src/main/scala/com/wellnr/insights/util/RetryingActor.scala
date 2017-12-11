package com.wellnr.insights.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RetryingActor {
  case class Retry(originalSender: ActorRef, message: Any, times: Int)

  case class Response(originalSender: ActorRef, result: Any)
}

case class RetryingActor(retries: Int, timeout: FiniteDuration, interval: FiniteDuration, forwardTo: ActorRef) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  import RetryingActor._

  def retry: Receive = {
    case Response(originalSender, result) =>
      originalSender ! result
      context stop self

    case Retry(originalSender, message, triesLeft) =>
      (forwardTo ? message) (timeout) onComplete {
        case Success(result) =>
          if (triesLeft < retries) {
            log.info(s"Received response from ${forwardTo} after ${retries - triesLeft} retries.")
          }

          self ! Response(originalSender, result)

        case Failure(ex) =>
          log.warning(s"Received an exception or timeout from ${forwardTo}. Remaining retries: ${triesLeft}")

          if (triesLeft <= 0) {
            self ! Response(originalSender, Failure(new Exception("Retries exceeded", ex)))
          } else {
            context.system.scheduler.scheduleOnce(interval, self, Retry(originalSender, message, triesLeft - 1))
          }
      }

    case message @ _ =>
      log.warning(s"Received unexpected message. Will not handle this message: ${message}")
  }

  def receive: Receive = {
    case message @ _ =>
      self ! Retry(sender, message, retries)
      context.become(retry, false)
  }

}
