name := "kuzminki-play-demo"
organization := "kuzminki.info"

version := "0.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,
  "io.github.karimagnusson" %% "kuzminki-ec" % "0.9.5-RC4",
  "io.github.karimagnusson" %% "kuzminki-ec-pekko" % "0.9.3",
  "io.github.karimagnusson" %% "kuzminki-pekko-play" % "0.9.3"
)