package org.{{ cookiecutter.project_subdomain }}.com.config

import scala.concurrent.duration.Duration

case class RateLimiter(rate: Int, duration: Duration)
