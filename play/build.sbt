name := "kuzminki-play-demo"
organization := "kuzminki.info"

version := "0.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,
  "io.github.karimagnusson" %% "kuzminki-play" % "0.9.5"
)