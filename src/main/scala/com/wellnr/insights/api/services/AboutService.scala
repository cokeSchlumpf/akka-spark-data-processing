package com.wellnr.insights.api.services

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json
import spray.json.{DefaultJsonProtocol, PrettyPrinter}

case class AboutService(instanceName: String, version: String) extends SprayJsonSupport with DefaultJsonProtocol {

  final case class About(instanceName: String, version: String)

  implicit val printer: json.PrettyPrinter.type = PrettyPrinter

  implicit val itemFormat = jsonFormat2(About)

  val route: Route =
    get {
      complete(About(instanceName, version))
    }

}
