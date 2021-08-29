package org.example.com.e2e

import com.twitter.finagle.http.Status
import org.example.com.e2e.support.TestApplication
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import io.circe.optics.JsonPath.root
import org.scalatest.matchers.must.Matchers

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers {
  Scenario("Server is running and servers endpoint") {
    withTestApp() { context =>
      val response = context.executeRequest("/get/success")

      response.statusCode mustBe Status.Ok.code
      response.contentType mustBe Some("application/json")
    }
  }

  Scenario("Metrics are recorded") {
    withTestApp() { implicit context =>
      context.executeRequest("/get/success")
      context.executeRequest("/get/failure")

      val Right(metrics) = fetchMetrics()

      root.meters.retrievals.count.long.getOption(metrics) mustBe Some(2)
      root.meters.`item-repository.failure`.count.long.getOption(metrics) mustBe Some(1)
      root.meters.`item-repository.success`.count.long.getOption(metrics) mustBe Some(1)
    }
  }
}
