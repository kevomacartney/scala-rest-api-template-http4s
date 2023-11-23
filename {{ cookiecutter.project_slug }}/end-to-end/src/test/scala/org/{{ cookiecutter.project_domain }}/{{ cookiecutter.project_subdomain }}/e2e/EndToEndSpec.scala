<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/e2e/EndToEndSpec.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e

import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e.support.TestApplication
========
package org.{{ cookiecutter.project_subdomain }}.com.e2e

import org.{{ cookiecutter.project_subdomain }}.com.e2e.support.TestApplication
>>>>>>>> main:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_subdomain }}/com/e2e/EndToEndSpec.scala
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import io.circe.optics.JsonPath.root
import org.scalatest.matchers.must.Matchers
import org.http4s._
import org.http4s.headers.`Content-Type`

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers {

  Scenario("Server is running and serves endpoints") {
    withTestApp() { context =>
      context.executeRequestWithResponse("/get/success") { response =>
        response.status mustBe Status.Ok
        response.contentType mustBe Some(`Content-Type`(MediaType.application.json))
      }
    }
  }

  Scenario("Metrics are recorded") {
    withTestApp() { implicit context =>
      context.executeRequestWithResponse("/get/success")(_ => ())
      context.executeRequestWithResponse("/get/failure")(_ => ())

      val metrics = fetchMetrics()

      root.meters.retrievals.count.long.getOption(metrics) mustBe Some(2)
      root.meters.`item-repository.failure`.count.long.getOption(metrics) mustBe Some(1)
      root.meters.`item-repository.success`.count.long.getOption(metrics) mustBe Some(1)
    }
  }
}
