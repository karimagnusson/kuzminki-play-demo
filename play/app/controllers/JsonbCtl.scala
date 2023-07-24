package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import utils._
import models.world._
import modules.KuzminkiPlay
import kuzminki.api._
import kuzminki.fn._

// Examples for jsonb field.

@Singleton
class JsonbCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
)(implicit ec: ExecutionContext) extends BaseController
                                    with PlayJson {

  implicit val db = kuzminkiPlay.db

  val countryData = Model.get[CountryData]

  def jsonbCountry(code: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.id,
        t.code,
        t.langs,  // array field
        t.data    // jsonb field
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def jsonbCapital(name: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.id,
        t.code,
        t.langs,
        (t.data || t.cities).as("data") // add cities to data
      ))
      .where(_.data -> "capital" ->> "name" === name)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def jsonbCityPopulation = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.id,
        t.code,
        (t.data ->> "name").as("name"),
        (t.cities -> "cities" -> 0).as("largest_city")
      ))
      .where(t => (t.cities -> "cities" -> 0 ->> "population").isNotNull)
      .orderBy(t => (t.cities -> "cities" -> 0 ->> "population").asInt.desc)
      .limit(5)
      .runAs[JsValue]
      .map(jsonList)

  }

  def jsonbCapitalAvg(cont: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        Agg.avg((t.data #>> Seq("capital", "population")).asInt)
      ))
      .where(t => Seq(
        (t.data #>> Seq("capital", "population")).isNotNull,
        t.data ->> "continent" === cont
      ))
      .runHeadAs[JsValue]
      .map(jsonObj)
  }

  def addPhone = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val phone = (request.body \ "phone").as[String]

    sql
      .update(countryData)
      .set(_.data += Json.obj("phone" -> phone)) // add "phone" to object
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.id,
        t.data
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def delPhone = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]

    sql
      .update(countryData)
      .set(_.data -= "phone") // remove "phone" from the object
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.id,
        t.data
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}









