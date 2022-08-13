package kuzminki.module

import javax.inject._
import scala.concurrent.Future
import akka.actor.ActorSystem
import play.api.Configuration
import kuzminki.api.{Kuzminki, DbConfig}


object KuzminkiPlay {

  private var dbOpt: Option[Kuzminki] = None
  
  def start(config: Configuration, system: ActorSystem): Kuzminki = {
    dbOpt match {
      case Some(db) => db
      case None =>
        val dbConf = DbConfig
          .forDb(config.get[String]("kuzminki.db"))
          .withDispatcher(config.get[String]("kuzminki.dispatcher"))
          .withHost(config.get[String]("kuzminki.host"))
          .withPort(config.get[String]("kuzminki.port"))
          .withUser(config.get[String]("kuzminki.user"))
          .withPassword(config.get[String]("kuzminki.password"))
        dbOpt = Some(Kuzminki.create(dbConf)(system))
        dbOpt.get
    }
  }

  def stop(): Future[Unit] = {
    dbOpt match {
      case Some(db) =>
        dbOpt = None
        db.close
      case None =>
        Future.successful(())
    }
  }
}


class KuzminkiPlay @Inject() (config: Configuration, system: ActorSystem) {
  val db = KuzminkiPlay.start(config, system)
}











