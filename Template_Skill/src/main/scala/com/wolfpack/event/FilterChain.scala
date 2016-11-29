package com.wolfpack.event

import java.sql.Date

class FilterChain(filters: List[Filter]) {
  import FilterChain._

  def apply(query: EventsQuery): EventsQuery = {
    applyFilters(filters, query)
  }
}

object FilterChain {
  import com.wolfpack.database.{Tables, MyPostgresDriver}
  import slick.lifted.Query

  import Tables._
  import MyPostgresDriver.api._

  type EventsQuery = Query[Events, EventsRow, Seq]

  def applyFilters(filters: List[Filter], query: EventsQuery): EventsQuery = {
    filters match {
      case List() => query
      case f :: fs => applyFilters(fs, applyFilter(f, query))
    }
  }

  def applyFilter(filter: Filter, query: EventsQuery): EventsQuery = {
    filter match {
      case CategoryFilter("all") => query
      case CategoryFilter(category) => for {
        c <- Categories if c.name === category
        ec <- EventCategories if ec.categoryId === c.categoryId
        e <- query if e.eventId === ec.eventId
      } yield e

      case StartFilter(start, end) =>
        query.filter(e => e.start.asColumnOf[Date] >= start &&
          e.start.asColumnOf[Date] < end)

      case TitleFilter(title) => query.filter(_.title === title)
    }
  }
}
