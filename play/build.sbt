name := "kuzminki-demo"
organization := "kuzminki.io"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  guice,
  //"org.scala-lang" % "scala-reflect" % "2.13.8",
  //"org.postgresql" % "postgresql" % "42.2.24",
  //"com.zaxxer" % "HikariCP" % "4.0.3" // uncomment for for own build
  "io.github.karimagnusson" % "kuzminki-ec" % "0.9.4-RC6"
)
