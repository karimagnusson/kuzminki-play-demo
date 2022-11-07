package utils

import play.api._
import play.api.mvc._
import play.api.http.HttpEntity
import akka.util.ByteString
import play.api.libs.json._
import kuzminki.api.Jsonb


trait PlayJson {

  implicit val loadJson: Seq[Tuple2[String, Any]] => JsValue = { data =>
    PlayJsonLoader.load(data)
  }

  implicit val jsValueToJsonb: JsValue => Jsonb = obj => {
    Jsonb(Json.stringify(obj))
  }

  implicit val jsonbTojsValue: Jsonb => JsValue = obj => {
    Json.parse(obj.value)
  }

  def ok(obj: JsValue) = Result(
    header = ResponseHeader(200, Map.empty),
    body = HttpEntity.Strict(
      ByteString(Json.stringify(obj)),
      Some("application/json")
    )
  )
  
  val notFound = Json.obj("message" -> "not found")
  val okTrue = Json.obj("ok" -> true)

  val jsonObj: JsValue => Result = obj => ok(obj)

  val jsonOpt: Option[JsValue] => Result = {
    case Some(obj) => ok(obj)
    case None => ok(notFound)
  }

  val jsonList: List[JsValue] => Result = list => ok(JsArray(list))

  val jsonOk: Unit => Result = _ => jsonObj(okTrue)
}