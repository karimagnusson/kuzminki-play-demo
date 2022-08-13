package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import models.worlddb._
import models.PlayJsonLoader
import kuzminki.module.KuzminkiPlay
import kuzminki.api._
import kuzminki.fn._


@Singleton
class CountryController @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
) (implicit ec: ExecutionContext) extends BaseController {

  implicit val db = kuzminkiPlay.db
  implicit val loadJson: Seq[Tuple2[String, Any]] => JsValue = { data =>
    PlayJsonLoader.load(data)
  }

  val city = Model.get[City]
  val country = Model.get[Country]

  val notFound = Json.obj("message" -> "not found")

  def countryResult1(code: String) = Action.async {
    sql
      .select(country)
      .colsType(_.slim)
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(notFound)
      }
  }

  def countryResult2(code: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map {
        case Some(res) => Ok(res)
        case None => Ok(notFound)
      }
  }

  def countryResult3(code: String) = Action.async {
    sql
      .select(country)
      .cols4(t => (
        t.code,
        t.name,
        t.continent,
        t.region
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map {
        case Some(res) => res match {
          case (code, name, continent, region) =>
            Ok(Json.obj(
              "code" -> code,
              "name" -> name,
              "continent" -> continent,
              "region" -> region
            ))
        }
        case None => Ok(notFound)
      }
  }

  def topCities(code: String) = Action.async {
    sql
      .select(city, country)
      .colsNamed(t => Seq(
        t.a.countryCode,
        t.a.population,
        "city_name" -> t.a.name,
        "country_name" -> t.b.name,
        t.b.continent,
        t.b.region
      ))
      .joinOn(_.countryCode, _.code)
      .where(_.b.code === code.toUpperCase)
      .orderBy(_.a.population.desc)
      .limit(10)
      .runAs[JsValue]
      .map(JsArray(_))
      .map(Ok(_))
  }

  def countryOptional = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(country)
      .colsNamed(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region,
        t.population
      ))
      .whereOpts(t => Seq(
        t.continent === params.get("continent"),
        t.region === params.get("region"),
        t.population > params.get("population_gt").map(_.toInt),
        t.population < params.get("population_lt").map(_.toInt)
      ))
      .orderBy(_.name.asc)
      .limit(10)
      .runAs[JsValue]
      .map(res => Ok(Json.toJson(res)))
  }

  def countryAndOr(continent: String) = Action.async {
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
        t.continent === continent,
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
      .map(res => Ok(Json.toJson(res)))
  }

  def continentPopulation(continent: String) = Action.async {
    sql
      .select(country)
      .colsNamed(t => Seq(
        "count" -> Count.all,
        "avg" -> Avg.int(t.population),
        "max" -> Max.int(t.population),
        "min" -> Min.int(t.population)
      ))
      .where(_.continent === continent)
      .runHeadAs[JsValue]
      .map(Ok(_))
  }
}












