package models

import play.api.libs.json._

import java.time._
import io.rdbc.sapi.DecimalNumber
import models.modeltypes._
import kuzminki.api._


object dvdrental {

  class Actor extends Model("actor") {
    val actorId = column[Int]("actor_id")
    val firstName = column[String]("first_name")
    val lastName = column[String]("last_name")
    val lastUpdate = column[Instant]("last_update")

    def idAndName = (actorId, firstName, lastName)
    def info = read[ActorInfo](actorId, firstName, lastName, lastUpdate)
    def addUser = write[AddActor](firstName, lastName)
  }

  Model.register[Actor]

  class Film extends Model("film") {
    val filmId = column[Int]("film_id")
    val title = column[String]("title")
    val description = column[String]("description")
    val releaseYear = column[Short]("release_year")
    val length = column[Short]("length")
    val languageId = column[Int]("language_id")

    def all = (filmId, title, description, releaseYear, length)
  }

  Model.register[Actor]

  class FilmActor extends Model("film_actor") {
    val actorId = column[Int]("actor_id")
    val filmId = column[Int]("film_id")
  }

  Model.register[Actor]

  class Language extends Model("language") {
    val languageId = column[Int]("language_id")
    val name = column[String]("name")
  }

  Model.register[Language]

  class Customer extends Model("customer") {
    val customerId = column[Int]("customer_id")
    val storeId = column[Short]("store_id")
    val firstName = column[String]("first_name")
    val lastName = column[String]("last_name")
    val email = column[String]("email")
    val addressId = column[Short]("address_id")
    val isActive = column[Boolean]("activebool")
    val lastUpdate = column[Instant]("last_update")
  }

  Model.register[Customer]

  class Address extends Model("address") {
    val addressId = column[Int]("address_id")
    val address = column[String]("address")
    val address2 = column[String]("address2")
    val district = column[String]("district")
    val cityId = column[Short]("city_id")
    val postalCode = column[String]("postal_code")
    val phone = column[String]("phone")
    val lastUpdate = column[Instant]("last_update")
  }

  Model.register[Address]

  class Rental extends Model("rental") {
    val rentalId = column[Int]("rental_id")
    val customerId = column[Int]("customer_id")
    val rentalDate = column[Instant]("rental_date")
    val returnDate = column[Instant]("return_date")
  }

  Model.register[Rental]

  class Store extends Model("store") {
    val storeId = column[Int]("store_id")
    val managerStaffId = column[Short]("manager_staff_id")
    val addressId = column[Short]("address_id")
    val lastUpdate = column[Instant]("last_update")

    def all = (storeId, managerStaffId, addressId, lastUpdate)
  }

  Model.register[Store]

  class Payment extends Model("payment") {
    val paymentId = column[Int]("payment_id")
    val customerId = column[Short]("customer_id")
    val rentalId = column[Int]("rental_id")
    val amount = column[DecimalNumber]("amount")
    val paymentlDate = column[Instant]("payment_date")
  }

  Model.register[Payment]

  class Staff extends Model("staff") {
    val staffId = column[Int]("staff_id")
    val firstName = column[String]("first_name")
    val lastName = column[String]("last_name")
    val addressId = column[Int]("address_id")
    val email = column[String]("email")
    val storeId = column[Short]("store_id")
    val isActive = column[Boolean]("active")
    val username = column[String]("username")
    val password = column[String]("password")
    val lastUpdate = column[Instant]("last_update")

    val info = read[StaffInfo](staffId, username, email, storeId, isActive)
  }

  Model.register[Staff]
}























