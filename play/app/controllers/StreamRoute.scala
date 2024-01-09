package controllers

import java.util.UUID
import java.sql.Timestamp
import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http.HttpEntity
import scala.util.{Try, Success, Failure}
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.actor.ActorSystem
import kuzminki.api._
import kuzminki.fn._
import kuzminki.pekko.stream._
import demo.responses.PlayJsonDemo
import models.world._

// Examples for streaming.

@Singleton
class StreamRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           as: ActorSystem,
           db: Kuzminki) extends BaseController
                            with PlayJsonDemo {

  val coinPrice = Model.get[CoinPrice]
  val tempCoinPrice = Model.get[TempCoinPrice]

  val makeLine: Tuple3[String, String, Timestamp] => String = {
    case (coin, price, takenAt) =>
      "%s,%s,%s\n".format(coin, price, takenAt.toString)
  }

  val splitLine = Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)

  val parseLine: String => Tuple3[String, BigDecimal, Timestamp] = { line =>
    line.split(',') match {
      case Array(coin, price, takenAt) =>
        (coin, BigDecimal(price), Timestamp.valueOf(takenAt))
      case _ =>
        throw new Exception("invalid file")
    }
  }

  val insertCoinPriceStm = sql
    .insert(coinPrice)
    .cols3(t => (
      t.coin,
      t.price,
      t.created
    ))
    .cache

  val insertTempCoinPriceStm = sql
    .insert(tempCoinPrice)
    .cols4(t => (
      t.uid,
      t.coin,
      t.price,
      t.created
    ))
    .cache

  // Stream data from the database to the client.

  def streamExport(code: String) = Action {
    val source = sql
      .select(coinPrice)
      .cols3(t => (
        t.coin,
        Fn.roundStr(t.price, 2),
        t.created
      ))
      .where(_.coin === code.toUpperCase)
      .orderBy(_.created.asc)
      .asSource
      .map(makeLine)
      .map(ByteString(_))

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, None, Some("text/csv"))
    )
  }

  // Stream file contents to the database.

  def streamImport = Action.async(parse.temporaryFile) { request =>
    FileIO
      .fromPath(request.body.path)
      .via(splitLine)
      .map(_.utf8String)
      .map(parseLine)
      .runWith(insertCoinPriceStm.asSink)
      .map(jsonSuccess)
  }

  // Stream file contents into a temporary table. If successful, move the rows to the target table, 
  // finally delete the rows from the temp table.

  def streamSafeImport = Action.async(parse.temporaryFile) { request =>
    val uid = UUID.randomUUID
    val addUid: Tuple3[String, BigDecimal, Timestamp] => Tuple4[UUID, String, BigDecimal, Timestamp] = {
      case (code, price, created) => (uid, code, price, created)
    }

    (for {

      _ <- FileIO
        .fromPath(request.body.path)
        .via(splitLine)
        .map(_.utf8String)
        .map(parseLine)
        .map(addUid)
        .grouped(100) // insert 100 in each transaction.
        .runWith(insertTempCoinPriceStm.asBatchSink)

      _ <- sql
        .insert(coinPrice)
        .cols3(t => (
          t.coin,
          t.price,
          t.created
        ))
        .fromSelect(
          sql
            .select(tempCoinPrice)
            .cols3(t => (
              t.coin,
              t.price,
              t.created
            ))
            .where(_.uid === uid)
        )
        .run

    } yield ()).transformWith { res =>
      val rsp = res match { 
        case Success(_) => jsonSuccess(())
        case Failure(ex) => jsonError(ex)
      }
      for {
        _ <- sql
          .delete(tempCoinPrice)
          .where(_.uid === uid)
          .run
      } yield rsp
    }
  }
}




















