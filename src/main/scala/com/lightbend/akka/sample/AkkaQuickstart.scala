//#full-example
package com.lightbend.akka.sample

import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.{ExecutionContext, Future}

class BlockingFutureActor extends Actor {
  implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-blocking-dispatcher")

  def receive = {
    case i: Int =>
      // println("Calling blocking future.")
      Future {
        Thread.sleep(5000)
        if (i % 100 == 0) println(s"Blocking future finished $i")
      }
  }
}

class PrintingActor extends Actor {
  def receive = {
    case i: Int =>
      println(s"Printing actor $i")
  }
}

object AkkaQuickstart extends App {
  // Create the 'helloAkka' actor system
  val system: ActorSystem = ActorSystem("helloAkka")

  val actor1 = system.actorOf(Props(new BlockingFutureActor()))
  val actor2 = system.actorOf(Props(new PrintingActor))

  for (i <- 1 to 500) {
    actor1 ! i
    actor2 ! i
  }

}
