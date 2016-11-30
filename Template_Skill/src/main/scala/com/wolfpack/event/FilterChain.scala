package com.wolfpack.event

import java.sql.Date

import java.util.{List => JList, Map => JMap}

import scala.collection.JavaConverters._


class FilterChain(filters: List[Filter]) {
  import FilterChain._

  def this(attrib: Object) = this(
    Option(attrib) match {
      case Some(objs) =>
        val blobs = objs.asInstanceOf[JList[JMap[String, Object]]]
        blobs.asScala.toList.map(Filter.load)

      case None =>
        List.empty
    }
  )

  def append(filter: Filter): FilterChain = {
    new FilterChain(filters :+ filter)
  }
  def apply(query: EventsQuery): EventsQuery = applyFilters(filters, query)
  def toAttrib: JList[Filter] = filters.asJava
}

object FilterChain {
  import com.wolfpack.database.{Tables, MyPostgresDriver}
  import slick.lifted.Query

  import MyPostgresDriver.api._
  import Tables._

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
