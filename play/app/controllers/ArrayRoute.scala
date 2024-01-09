package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import kuzminki.api._
import kuzminki.pekko.play.json.PlayJson
import demo.responses.PlayJsonDemo
import models.world._

// Examples for array field.

@Singleton
class ArrayRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: Kuzminki) extends BaseController
                            with PlayJson // implicit conversion to Json
                            with PlayJsonDemo {

  val countryData = Model.get[CountryData]

  // Select a row with an array field.

  def arrayLangs(code: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.code,
        t.langs
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Add to the array, make sure "lang" occurs once and sort the array ASC.

  def arrayAdd = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val lang = (request.body \ "lang").as[String]

    sql
      .update(countryData)
      .set(_.langs addAsc lang)
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.code,
        t.langs
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Remove all instances of "lang" from the array.

  def arrayDel = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val lang = (request.body \ "lang").as[String]

    sql
      .update(countryData)
      .set(_.langs -= lang)
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.code,
        t.langs
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}