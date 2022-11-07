package modules

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
        val db = Kuzminki.create(
          DbConfig
            .forDb(config.get[String]("kuzminki.db"))
            .withHost(config.get[String]("kuzminki.host"))
            .withPort(config.get[Int]("kuzminki.port"))
            .withUser(config.get[String]("kuzminki.user"))
            .withPassword(config.get[String]("kuzminki.password"))
            .withPoolSize(config.get[Int]("kuzminki.poolsize")),
          system.dispatchers.lookup(
            config.get[String]("kuzminki.dispatcher")
          )
        )(system.dispatchers.defaultGlobalDispatcher)
        dbOpt = Some(db)
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











