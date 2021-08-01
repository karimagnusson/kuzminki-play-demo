package models

import play.api.libs.json._

import java.time._
import io.rdbc.sapi.DecimalNumber
import models.dvdrental._
import kuzminki.api._
import kuzminki.fn._


object modeltypes {

  case class AddActor(
    firstName: String,
    lastName: String
  )

  case class ActorInfo(
    actor_id: Int,
    first_name: String,
    last_name: String,
    last_update: Instant
  )

  implicit val actorInfoWrites = Json.writes[ActorInfo]

  case class StaffInfo(
    staff_id: Int,
    username: String,
    email: String,
    store_id: Short,
    is_active: Boolean
  )

  implicit val staffInfoWrites = Json.writes[StaffInfo]

  case class CustomerPayment(id: Int, name: String, amount: String)
  implicit val customerPaymentWrites = Json.writes[CustomerPayment]

  class CustomerPaymentJoin extends ExtendedJoin[Customer, Payment] {
    val columns = read[CustomerPayment](
      a.customerId,
      Fn.concatWs(" ", a.firstName, a.lastName),
      Round.numeric(b.amount, 2).asString
    )
  }

  implicit val toCustomerPaymentJoin = Join.register[CustomerPaymentJoin, Customer, Payment]
  val customerPaymentJoin = Join.get[CustomerPaymentJoin]
}