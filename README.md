[![Gitter](https://img.shields.io/gitter/room/rdbc-io/rdbc.svg?style=flat-square)](https://gitter.im/kuzminki/community)

# kuzminki-play-demo

kuzminki-play-demo is a demo project for the [kuzminki](https://github.com/karimagnusson/kuzminki) Scala/PostgreSQL database access library. It offers HTTP endpoints using the [Play](https://github.com/playframework/playframework) web framework and allows you to familiarize yourself and play around with how to build queries.

If you run into any problems feel free to post on [Gitter](https://gitter.im/kuzminki/community) or contact me directly on telegram @karimagnusson.

#### Setup

```sbt
sbt new playframework/play-scala-seed.g8
```

Then replace `app`, `conf`, `build.sbt` with the ones in this project.

Also move the folder `csv` into the root of the new project.

#### Database

Download and setup [PostgreSQL Sample Database](https://www.postgresqltutorial.com/postgresql-sample-database/)

#### Config `conf/application.conf`

```sbt
kuzminki = {
  host = "localhost"
  port = 5432
  user = "<USERNAME>"
  password = "<PASSWORD>"
  db = "dvdrental"
  threads = 10
}
```

#### Postman

If you use [Postman](https://www.postman.com/) you can import `postman/play-demo.json` where all the endpoints are set up.

#### Run

```sbt
sbt run
```