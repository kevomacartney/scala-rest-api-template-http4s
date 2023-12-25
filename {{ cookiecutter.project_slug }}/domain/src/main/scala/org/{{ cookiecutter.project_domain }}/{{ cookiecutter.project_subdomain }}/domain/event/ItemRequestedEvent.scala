package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event

import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.joda.time.{DateTime, DateTimeZone}
import org.foxi.account.ItemRequested

case class ItemRequestedEvent(
    id: String,
    name: String,
    at: Long = DateTime.now(DateTimeZone.UTC).getMillis
) extends Event[DomainItem]

object ItemRequestedEvent {
  def mapToAvroRecord(event: ItemRequestedEvent): ItemRequested = {
    ItemRequested(
      id = event.id,
      name = event.name,
      at = event.at
    )
  }
}
