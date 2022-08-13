# kuzminki-play-demo

kuzminki-play-demo is a demo project for the [kuzminki-akka](https://github.com/karimagnusson/kuzminki-akka) Scala/PostgreSQL database access library. It offers HTTP endpoints using the [Play](https://github.com/playframework/playframework) web framework and allows you to familiarize yourself and play around with how to make database requests.

#### Setup

```sbt
sbt new playframework/play-scala-seed.g8
```

Then replace `app`, `conf`, `build.sbt` with the ones in this project.

#### Database

```sql
CREAE DATABASE world;
```

```bash
psql -d world < world.sql
```

#### Config `conf/application.conf`
Replace username and password
```sbt
user = "<USERNAME>"
password = "<PASSWORD>"
```

#### Postman

If you use [Postman](https://www.postman.com/) you can import `postman/play-demo.json` where all the endpoints are set up.

#### Run

```sbt
sbt run
```