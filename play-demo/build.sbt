name := "kuzminki-play-demo"

scalaVersion := "2.12.12"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "io.github.karimagnusson" % "kuzminki" % "0.9.0",
  "io.github.karimagnusson" % "kuzminki-play-json" % "0.9.1"
)


