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

/*
  These are the same queries as in SelectDbJsonCtl
  but here the databse returns the each row as Tuple2[String, Any]
  that is transformed into JsValue with utils.PlayJsonLoader.
  Note the trait PlayJson.
*/

@Singleton
class SelectPlayJsonCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
)(implicit ec: ExecutionContext) extends BaseController
                                    with PlayJson {

  implicit val db = kuzminkiPlay.db

  val city = Model.get[City]
  val country = Model.get[Country]
  val lang = Model.get[Lang]

  def selectCountry(code: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue] // read into JsValue with utils.PlayJsonLoader
      .map(jsonOpt(_))
  }

  def selectCities(code: String) = Action.async {
    sql
      .select(city, country)
      .colsNamed(t => Seq(
        t.a.countryCode,
        t.a.population,          // use column name
        "city_name" -> t.a.name, // define the name
        "country_name" -> t.b.name,
        t.b.continent,
        t.b.region
      ))
      .joinOn(_.countryCode, _.code)
      .where(_.b.code === code.toUpperCase)
      .orderBy(_.a.population.desc)
      .limit(5)
      .runAs[JsValue]
      .map(jsonList(_))
  }

  def selectLanguage(code: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        sql             // subquery as a nested object
          .select(lang)
          .colsJson(s => Seq(
            s.language,
            s.percentage
          ))
          .where(s => Seq(
            s.countryCode <=> t.code,
            s.isOfficial === true
          ))
          .limit(1)
          .asColumn
          .first
          .as("language")
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def selectCountryCities(code: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        Fn.json(Seq(    // put some columns in a nested object
          t.continent,
          t.region,
          t.population
        )).as("info"),
        sql             // subquery as a array of objects
          .select(city)
          .colsJson(s => Seq(
            s.name,
            s.population
          ))
          .where(_.countryCode <=> t.code)
          .orderBy(_.population.desc)
          .limit(5)
          .asColumn
          .as("cities")
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def selectOptional = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region,
        t.population
      ))
      .whereOpt(t => Seq(  // optional filters
        t.continent === params.get("cont"),
        t.region === params.get("region"),
        t.population > params.get("pop_gt").map(_.toInt),
        t.population < params.get("pop_lt").map(_.toInt)
      ))
      .orderBy(_.name.asc)
      .limit(10)
      .runAs[JsValue]
      .map(jsonList(_))
  }

  def selectAndOr(cont: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region,
        t.population,
        t.surfaceArea,
        t.lifeExpectancy,
        t.gnp
      ))
      .where(t => Seq(
        t.continent === cont,
        Or(
          And(
            t.population > 20000000,
            t.surfaceArea > 500000
          ),
          And(
            t.lifeExpectancy > 65,
            t.gnp > 150000
          )
        )
      ))
      .orderBy(_.name.asc)
      .limit(10)
      .runAs[JsValue]
      .map(jsonList(_))
  }

  def selectPopulation(cont: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        "count" -> Count.all,
        "avg" -> Agg.avg(t.population),
        "max" -> Agg.max(t.population),
        "min" -> Agg.min(t.population)
      ))
      .where(_.continent === cont)
      .runHeadAs[JsValue]
      .map(jsonObj(_))
  }
}












