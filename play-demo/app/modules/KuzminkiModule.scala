package kuzminki.module

import javax.inject.Inject
import com.google.inject.AbstractModule
import play.api.inject.ApplicationLifecycle


class KuzminkiPlayModule extends AbstractModule {
  override def configure() = {
    bind(classOf[KuzminkiPlay]).asEagerSingleton()
  }
}


class KuzminkiPlayStop @Inject()(lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook { () =>
    KuzminkiPlay.stop()
  }
}









