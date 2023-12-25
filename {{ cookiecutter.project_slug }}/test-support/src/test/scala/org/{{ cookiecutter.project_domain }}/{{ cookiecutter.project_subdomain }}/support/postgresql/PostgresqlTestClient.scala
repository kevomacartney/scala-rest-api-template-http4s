package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._

class PostgresqlTestClient(transactor: Transactor[IO]) {
  def executeQuery[B](query: ConnectionIO[B]): B = {
    query.transact(transactor).unsafeRunSync()
  }

  def executeQueryAsync[B](query: ConnectionIO[B]): IO[B] = {
    query.transact(transactor)
  }
}

object PostgresqlTestClient {
  def apply(host: String, username: String, password: String): PostgresqlTestClient = {
    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = host,
      user = username,
      password = password,
      None
    )

    new PostgresqlTestClient(transactor)
  }
}
