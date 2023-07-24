# kuzminki-play-demo

kuzminki-play-demo is an example REST API using [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) and [Play Framework](https://github.com/playframework/playframework).

This project uses Scala 2.13.8 and Play 2.8.18

Examples:
- Select, insert, update, delete
- Cached queries
- Subqueries
- Jsonb field
- Array field
- Date/Time methods

#### Setup

```sbt
sbt new playframework/play-scala-seed.g8
```

Then replace `app`, `conf`, `build.sbt` with the ones in this project.

#### Database

```sql
CREATE DATABASE world;
```

```bash
psql world < db/world.pg
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