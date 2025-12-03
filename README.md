[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/bukotsunikki.svg?style=social&label=Follow%20%40kuzminki_lib)](https://twitter.com/kuzminki_lib)

# kuzminki-play-demo

Kuzminki is a feature-rich query builder and access library for PostgreSQL. It focuses on productivity by providing readable transparent syntax and making Postgres features available through the API. Among those features are support for Array and Jsonb fields, and streaming to and from the database. See full documentation at [https://kuzminki.kotturinn.com/](https://kuzminki.kotturinn.com/).

kuzminki-play-demo is an example REST API using [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) and [Play Framework](https://www.playframework.com/). Most of the example queries in this demo return rows as Play JSON. There are also examples where rows are returned from the database as a JSON string. Kuzminki has the ability to build complex JSON objects on the database using subqueries and return the result as a JSON string that can be returned directly to the client. Otherwise, the user will have to make multiple database requests and then build the JSON object from the results.

This latest version uses Play 3.0.1 with [Pekko](https://pekko.apache.org/). To use [Kuzminki](https://kuzminki.info/) with [Play](https://www.playframework.com/) as a module it uses [kuzminki-play](https://github.com/karimagnusson/kuzminki-play). It depends on [kuzminki-ec](https://github.com/karimagnusson/kuzminki-ec) which is an agnostic version and requires only Scala ExecutionContext. Support for Pekko streams is added with [kuzminki-pekko](https://github.com/karimagnusson/kuzminki-pekko). Kuzminki can also be used with Play 2.9

You may also be interested in [io-path](https://github.com/karimagnusson/io-path). It is simple library for working with files and folders and comes with a Play module.

Feel free to send me a DM on Twitter if you have any questions.

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