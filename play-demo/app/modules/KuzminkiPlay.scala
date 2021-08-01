package kuzminki.module

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Configuration
import kuzminki.rdbc.{Driver, DriverPool}
import kuzminki.api.KuzminkiApi


object KuzminkiPlay {

  private var dbOpt: Option[Driver] = None
  
  def start(conf: Config, system: ActorSystem, ec: ExecutionContext): Driver = {
    dbOpt match {
      case Some(db) => db
      case None =>
        val pool = DriverPool.forConfig(conf, ec)
        val db = Driver.create(pool, system, ec)
        dbOpt = Some(db)
        db
    }
  }

  def stop(): Future[Unit] = {
    dbOpt match {
      case Some(db) =>
        dbOpt = None
        db.shutdown()
      case None =>
        Future.successful(())
    }
  }
}


class KuzminkiPlay @Inject() (config: Configuration,
                              system: ActorSystem,
                              ec: ExecutionContext) extends KuzminkiApi {

  protected val db = KuzminkiPlay.start(config.underlying.getConfig("kuzminki"), system, ec)
}











