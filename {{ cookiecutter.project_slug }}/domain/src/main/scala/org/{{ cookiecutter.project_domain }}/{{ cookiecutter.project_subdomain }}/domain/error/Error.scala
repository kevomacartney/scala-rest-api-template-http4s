package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event.Event

sealed abstract class Error(val cause: Throwable) extends Throwable(cause)

sealed abstract class KafkaError(override val cause: Throwable, errorMessage: String) extends Error(cause)

object KafkaError {
  case class KafkaProduceError(override val cause: Throwable, error: String, topic: String)
      extends KafkaError(
        cause = cause,
        errorMessage = s"Error while producing message, [error=$error]"
      )
}

sealed abstract class RepositoryError(override val cause: Throwable, val errorMessage: String) extends Error(cause)

object RepositoryError {
  case class UnexpectedRepositoryError(override val cause: Throwable, error: String, operation: String)
      extends RepositoryError(
        cause = cause,
        errorMessage = s"Unexpected error, [error=$error]"
      )
}
