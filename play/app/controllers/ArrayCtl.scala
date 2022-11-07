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


@Singleton
class ArrayCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
) (implicit ec: ExecutionContext) extends BaseController
                                     with PlayJson {

  implicit val db = kuzminkiPlay.db

  val countryData = Model.get[CountryData]

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