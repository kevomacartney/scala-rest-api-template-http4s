package endpoints

import cats.effect.{ContextShift, IO}
import com.codahale.metrics.MetricRegistry
import io.circe.{Decoder, jawn}
import io.circe.generic.semiauto.deriveDecoder
import org.example.com.ItemRepository
import org.example.com.repositories.RepositoryItem
import org.example.com.services.ApiService
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class ApiEndpointSpec extends AnyWordSpec with Matchers with ScalaFutures {
  import ApiEndpointSpec._

  implicit val cs: ContextShift[IO]        = IO.contextShift(ExecutionContext.global)
  implicit val repositoryItemDecoder       = deriveDecoder[RepositoryItem]
  implicit val repositoryItemEntityDecoder = jsonOf[IO, RepositoryItem]

  "GET/ get" should {
    "return a repository item" in {
      withService() { service =>
        val request  = buildSuccessRequest()
        val response = service(request)

        response.status mustBe Status.Ok
        response.body.map
      }
    }

    "returns a failure with invalid input" in {
      withService() { service =>
        val request  = buildFailureRequest()
        val response = futureToAsync[IO, Response](service(request)).unsafeRunSync()

        response.status mustBe Status.BadRequest
      }
    }

    "Metrics get recorded" in {
      val metricRegistry = new MetricRegistry()

      withService(metricRegistry) { service =>
        val request = buildSuccessRequest()
        futureToAsync[IO, Response](service(request)).unsafeRunSync()

        metricRegistry.meter("retrievals").getCount mustBe 1
      }
    }
  }
}

object ApiEndpointSpec {
  import io.circe.generic.auto._

  type Service = Request[IO] => Response[IO]

  def withService[T](metricRegistry: MetricRegistry = new MetricRegistry())(f: Service => T)(
      implicit cs: ContextShift[IO]
  ): T = {
    val repository = new ItemRepository(metricRegistry)
    val service    = new ApiService(repository)(metricRegistry).helloWorldService.orNotFound.ru
    f(service)
  }

  def buildSuccessRequest(): Request[IO] = ???

  def buildFailureRequest(): Request[IO] = ???
}
