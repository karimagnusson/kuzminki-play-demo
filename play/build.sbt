name := "kuzminki-demo"
organization := "io.kuzminki"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "io.github.karimagnusson" % "kuzminki-akka" % "0.9.4-RC1"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.kuzminki.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.kuzminki.binders._"
