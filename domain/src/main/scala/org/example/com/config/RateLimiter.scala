package org.example.com.config

import scala.concurrent.duration.Duration

case class RateLimiter(rate: Int, duration: Duration)
