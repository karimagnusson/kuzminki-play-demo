# kuzminki-play-demo

##### Setup

```sbt
sbt new playframework/play-scala-seed.g8
```

Then replace `app`, `conf`, `build.sbt`.
Also move the folder `csv` into the root of the new project.

##### Database

###### Download and setup [PostgreSQL Sample Database](https://www.postgresqltutorial.com/postgresql-sample-database/)

##### Config `conf/application.conf`

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

##### Postman

###### If you use [Postman](https://www.postman.com/) you can import `postman/play-demo.json` where all the endpoints are set up.

