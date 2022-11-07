package utils

import play.api._
import play.api.mvc._
import play.api.http.HttpEntity
import akka.util.ByteString
import kuzminki.api.Jsonb


trait DbJson {

  def ok(jsString: String) = Result(
    header = ResponseHeader(200, Map.empty),
    body = HttpEntity.Strict(
      ByteString(jsString),
      Some("application/json")
    )
  )
  
  val notFound = """{"message": "not found"}"""
  val okTrue = """{"ok": true}"""

  val jsonObj: Jsonb => Result = obj => ok(obj.value)

  val jsonOpt: Option[Jsonb] => Result = {
    case Some(obj) => ok(obj.value)
    case None => ok(notFound)
  }

  val jsonList: List[Jsonb] => Result = { list =>
    ok("[%s]".format(list.map(_.value).mkString(",")))
  }

  val jsonOk: Unit => Result = _ => ok(okTrue)
}