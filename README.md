[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/bukotsunikki.svg?style=social&label=Follow%20%40kuzminki_lib)](https://twitter.com/kuzminki_lib)

# kuzminki-play-demo

kuzminki-play-demo is an example REST API using [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) and [Play Framework](https://github.com/playframework/playframework).

This latest version uses Play 3.0.1 with [Pekko](https://pekko.apache.org/). To integrate [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) as a module it uses [kuzminki-pekko-play](https://github.com/karimagnusson/kuzminki-play). This version has an added exmple of streaming. [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) is agnostic and requires only Scala ExecutionContext. Support for Pekko streams is added with [kuzminki-ec-pekko](https://github.com/karimagnusson/kuzminki-ec-stream).

Examples:
- Select, insert, update, delete
- Cached queries
- Subqueries
- Jsonb field
- Array field
- Date/Time methods
- Streaming

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