package endpoints

import cats.effect.{ContextShift, IO}
import com.codahale.metrics.MetricRegistry
import com.twitter.finagle.Service
import com.twitter.finagle.http._
import io.catbird.util.effect.futureToAsync
import io.circe.{Decoder, jawn}
import io.circe.generic.semiauto.deriveDecoder
import org.example.com.ItemRepository
import org.example.com.endpoints.ApiEndpoint
import org.example.com.repositories.RepositoryItem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class ApiEndpointSpec extends AnyWordSpec with Matchers with ScalaFutures {
  import ApiEndpointSpec._

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "GET/ get" should {
    "return a repository item" in {
      withService() { service =>
        val request  = buildSuccessRequest()
        val response = futureToAsync[IO, Response](service(request)).unsafeRunSync()

        response.status mustBe Status.Ok
        jawn.decode[RepositoryItem](response.contentString)
      }
    }

    "returns a failure with invalid input" in {
      withService(){service =>
        val request = buildFailureRequest()
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
  import io.finch.circe._
  import org.example.com.endpoints.ApiEndpoint._

  implicit val repositoryItemDecoder: Decoder[RepositoryItem] = deriveDecoder[RepositoryItem]

  def withService[T](metricRegistry: MetricRegistry = new MetricRegistry())(f: Service[Request, Response] => T)(
      implicit cs: ContextShift[IO]
  ): T = {
    val repository = new ItemRepository(metricRegistry)
    val service    = new ApiEndpoint(repository, metricRegistry).endpoint.toService
    f(service)
  }

  def buildSuccessRequest(): Request = {
    RequestBuilder
      .create()
      .url("http://localhost:8080/get/pass")
      .buildGet()
  }

  def buildFailureRequest(): Request =  {
    RequestBuilder
      .create()
      .url("http://localhost:8080/get/failure")
      .buildGet()
  }
}
