name := "kuzminki-play-demo"
organization := "kuzminki.info"

version := "0.3"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,
  "io.github.karimagnusson" % "kuzminki-ec" % "0.9.4",
  "io.github.karimagnusson" % "kuzminki-ec-pekko" % "0.9.0",
  "io.github.karimagnusson" % "kuzminki-pekko-play" % "0.9.1"
)