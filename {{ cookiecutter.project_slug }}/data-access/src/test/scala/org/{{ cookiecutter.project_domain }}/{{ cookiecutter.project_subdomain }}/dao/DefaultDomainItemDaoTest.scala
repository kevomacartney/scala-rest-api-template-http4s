package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits._
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.DefaultDomainItemDaoTest.{createTable, getDomainItem, insertDomainItem, withDomainItemDao}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao.{DomainItemDTO, DomainItemDao}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql.{PostgresqlTestContext, PostgresqlSupport}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor4}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues}

import java.util.UUID

class DefaultDomainItemDaoTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach
    with TableDrivenPropertyChecks
    with PostgresqlSupport {

  override def afterEach(): Unit = {
    super.afterEach()

    val dropTableQuery = sql"DROP TABLE IF EXISTS domain_item_v1;"
    postgresqlTestClient.executeQuery(dropTableQuery.update.run)
    reEstablishPostgresqlConnection()
  }

  val getConnectivityIssues: TableFor4[String, Unit => Unit, String, Unit => Unit] =
    Table(
      ("testDescription", "toxic", "expected Exception", "Clean up"),
      (
        "throw exception when database is not available",
        _ => breakPostgresqlConnection(),
        "PSQLException",
        _ => reEstablishPostgresqlConnection()
      ),
      (
        "exception when database takes too long to respond",
        _ => addLatencyToxi(),
        "TimeoutException",
        _ => removeLatencyToxi()
      )
    )

  "DefaultDomainItemDao.get" when {
    "get, should return expected item from database" in {
      implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
      val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

      withPostgresql { implicit postgresqlContext =>
        createTable()
        insertDomainItem(domainItem)

        withDomainItemDao { domainItemDao =>
          val result = domainItemDao.get(domainItem.id).value.unsafeRunSync()

          result shouldBe Some(domainItem)
        }
      }
    }

    forAll(getConnectivityIssues) { (testDescription, applyToxic, expectedException, removeToxic) =>
      "facing connectivity issues" should {
        testDescription in {
          implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
          val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

          withPostgresql { implicit postgresqlContext =>
            createTable()
            insertDomainItem(domainItem)

            withDomainItemDao { domainItemDao =>
              applyToxic()
              val result = domainItemDao.get(domainItem.id).value.attempt.unsafeRunSync()
              removeToxic()

              result.left.value.getClass.getSimpleName shouldBe expectedException
            }
          }
        }
      }
    }
  }

  val insertConnectivityIssues: TableFor4[String, Unit => Unit, String, Unit => Unit] =
    Table(
      ("testDescription", "toxic", "expected Exception", "Clean up"),
      (
        "throw exception when database is not available",
        _ => breakPostgresqlConnection(),
        "PSQLException",
        _ => reEstablishPostgresqlConnection()
      ),
      (
        "exception when database takes too long to respond",
        _ => addLatencyToxi(),
        "TimeoutException",
        _ => removeLatencyToxi()
      )
    )

  "DefaultDomainItemDao.insert" when {
    forAll(insertConnectivityIssues) { (testDescription, applyToxic, expectedException, removeToxic) =>
      "facing connectivity issues" should {
        testDescription in {
          implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
          val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

          withPostgresql { implicit postgresqlContext =>
            createTable()

            withDomainItemDao { domainItemDao =>
              applyToxic()
              val result = domainItemDao.insert(domainItem).attempt.unsafeRunSync()
              removeToxic()

              result.left.value.getClass.getSimpleName shouldBe expectedException
            }
          }
        }
      }
    }

    "insert, should write item to the database" in {
      implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
      val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

      withPostgresql { implicit postgresqlContext =>
        createTable()

        withDomainItemDao { domainItemDao =>
          domainItemDao.insert(domainItem).unsafeRunSync()

          val writtenItem = getDomainItem(domainItem.id)
          writtenItem shouldBe Some(domainItem)
        }
      }
    }
  }

  val updateConnectivityIssues: TableFor4[String, Unit => Unit, String, Unit => Unit] =
    Table(
      ("testDescription", "toxic", "expected Exception", "Clean up"),
      (
        "throw exception when database is not available",
        _ => breakPostgresqlConnection(),
        "PSQLException",
        _ => reEstablishPostgresqlConnection()
      ),
      (
        "exception when database takes too long to respond",
        _ => addLatencyToxi(),
        "TimeoutException",
        _ => removeLatencyToxi()
      )
    )

  "DefaultDomainItemDao.update" when {
    forAll(updateConnectivityIssues) { (testDescription, applyToxic, expectedException, removeToxic) =>
      "facing connectivity issues" should {
        testDescription in {
          implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
          val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

          withPostgresql { implicit postgresqlContext =>
            createTable()
            insertDomainItem(domainItem)

            withDomainItemDao { domainItemDao =>
              applyToxic()
              val result = domainItemDao.update(domainItem).attempt.unsafeRunSync()
              removeToxic()

              result.left.value.getClass.getSimpleName shouldBe expectedException
            }
          }
        }
      }
    }

    "update, should update item in the database" in {
      implicit val meterRegistry: SimpleMeterRegistry = new SimpleMeterRegistry()
      val domainItem                                  = DomainItemDTO(UUID.randomUUID(), "test")

      withPostgresql { implicit postgresqlContext =>
        createTable()
        insertDomainItem(domainItem)

        withDomainItemDao { domainItemDao =>
          val updatedDomainItem = domainItem.copy(name = "updated")
          domainItemDao.update(updatedDomainItem).unsafeRunSync()

          val writtenItem = getDomainItem(domainItem.id)
          writtenItem shouldBe Some(updatedDomainItem)
        }
      }
    }
  }
}

object DefaultDomainItemDaoTest {
  def withDomainItemDao[A](f: DomainItemDao[IO] => A)(
    implicit postgresqlContext: PostgresqlTestContext,
    meterRegistry: MeterRegistry
  ): A = {
    val transactor = for {
      hikariConfig <- Resource.pure {
                       val config = new HikariConfig()
                       config.setDriverClassName("org.postgresql.Driver")
                       config.setJdbcUrl(s"${postgresqlContext.host}/${postgresqlContext.databaseName}")
                       config.setUsername(postgresqlContext.username)
                       config.setPassword(postgresqlContext.password)
                       config.setMetricRegistry(meterRegistry)
                       config
                     }
      xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig)
    } yield xa

    transactor
      .use { xa =>
        val domainItemDao = new DefaultDomainItemDao(xa, 100)
        IO(f(domainItemDao))
      }
      .unsafeRunSync()
  }

  def createTable()(implicit postgresqlContext: PostgresqlTestContext): Unit = {
    val createTableQuery =
      sql"""
             CREATE TABLE domain_item_v1 (
               id UUID PRIMARY KEY,
               name VARCHAR(255) NOT NULL
             );
             """

    postgresqlContext.postgresqlTestClient.executeQuery(createTableQuery.update.run)
  }

  def insertDomainItem(domainItem: DomainItemDTO)(implicit postgresqlContext: PostgresqlTestContext): Unit = {
    val insertQuery =
      sql"""
             INSERT INTO domain_item_v1 (id, name)
             VALUES (${domainItem.id}, ${domainItem.name});
             """

    postgresqlContext.postgresqlTestClient.executeQuery(insertQuery.update.run)
  }

  def getDomainItem(id: UUID)(implicit postgresqlContext: PostgresqlTestContext): Option[DomainItemDTO] = {
    val getByIdQuery =
      sql"""
             SELECT id, name
             FROM domain_item_v1
             WHERE id = $id
             """.query[DomainItemDTO]

    postgresqlContext.postgresqlTestClient.executeQuery[Option[DomainItemDTO]](getByIdQuery.option)
  }
}
