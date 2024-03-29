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

// Cached queries

@Singleton
class CacheRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: Kuzminki) extends BaseController
                            with PlayJson // implicit conversion to Json
                            with PlayJsonDemo {

  val city = Model.get[City]
  val country = Model.get[Country]
  val language = Model.get[Language]
  val trip = Model.get[Trip]

  // cached queries

  val selectCountryStm = sql
    .select(country)
    .colsNamed(t => Seq(
      t.code,
      t.name,
      t.continent,
      t.region
    ))
    .all
    .pickWhere1(_.code.use === Arg)
    .cache

  val selectJoinStm = sql
    .select(city, country)
    .colsNamed(t => Seq(
      t.a.code,
      t.a.population,
      "city_name" -> t.a.name,
      "country_name" -> t.b.name,
      t.b.gnp,
      t.b.continent,
      t.b.region
    ))
    .joinOn(_.code, _.code)
    .where(t => Seq(
      t.b.continent === "Asia",
      t.b.gnp.isNotNull
    ))
    .orderBy(_.a.population.desc)
    .limit(5)
    .pickWhere2(t => (
      t.b.population.use >= Arg,
      t.b.gnp.use >= Arg
    ))
    .cache

  val insertTripStm = sql
    .insert(trip)
    .cols2(t => (
      t.cityId,
      t.price
    ))
    .returningNamed(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  val updateTripStm = sql
    .update(trip)
    .pickSet1(_.price.use ==> Arg)
    .pickWhere1(_.id.use === Arg)
    .returningNamed(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  val deleteTripStm = sql
    .delete(trip)
    .pickWhere1(_.id.use === Arg)
    .returningJson(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  def selectCountry(code: String) = Action.async {
    selectCountryStm
      .runHeadOptAs[JsValue](code.toUpperCase)
      .map(jsonOpt(_))
  }

  def selectJoin(pop: Int, gnp: Int) = Action.async {
    selectJoinStm
        .runAs[JsValue](pop, BigDecimal(gnp))
        .map(jsonList(_))
  }

  def insertTrip = Action.async(parse.json) { request =>
    val cityId = (request.body \ "city_id").as[Int]
    val price = (request.body \ "price").as[Int]
    insertTripStm
      .runHeadAs[JsValue]((cityId, price))
      .map(jsonObj(_))
  }

  def updateTrip = Action.async(parse.json) { request =>
    val id = (request.body \ "id").as[Int]
    val price = (request.body \ "price").as[Int]
    updateTripStm
      .runHeadOptAs[JsValue](price, id)
      .map(jsonOpt(_))
  }

  def deleteTrip = Action.async(parse.json) { request =>
    val id = (request.body \ "id").as[Int]
    deleteTripStm
      .runHeadOptAs[JsValue](id)
      .map(jsonOpt(_))
  }
}















