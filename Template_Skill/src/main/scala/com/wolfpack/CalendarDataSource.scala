package com.wolfpack

import collection.JavaConversions._
import collection.JavaConverters._
import collection.mutable.HashMap

import com.wolfpack.database.{DbCredentials, MyPostgresDriver, Tables}
import com.wolfpack.event.{Filter, FilterChain}

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.util.{Success, Failure}

import java.util.{List => JList, Map => JMap}

import java.sql.Timestamp

import MyPostgresDriver.api._
import Tables._

object CalendarDataSource {
  import slick.jdbc.{GetResult => GR}
  case class Event(eventId: Short, title: String, start: Timestamp)
  implicit def GetResultEvent(implicit e0: GR[Short], e1: GR[String], e2: GR[Timestamp]): GR[Event] = GR{
    prs => import prs._
    Event.tupled((<<[Short], <<[String], <<[Timestamp]))
  }

  val db = Database.forURL(DbCredentials.jdbcurl, DbCredentials.sdriver)

  def getNextEvent: Option[List[Event]] = {
    val query = sql"""
      SELECT event_id, title, start FROM events
        WHERE start >= now()
        ORDER BY start ASC
        LIMIT 1"""
    Await.ready(db.run(query.as[Event]), 5 seconds).value match {
      case Some(result) => result match {
        case Success(value) => Some(value.toList)
        case Failure(_) => None
      }
      case None => None
    }
  }

  def getEventsWithFilters(filters: JList[Filter]): Option[List[Event]] = {
    val filtersList = collectionAsScalaIterable(filters).toList
    val filterChain = new FilterChain(filtersList)
    val query = Events.sortBy(_.start.asc)
    val filtered = filterChain(query)
    val results = filtered.map(e => (e.eventId, e.title, e.start)).result
    Await.ready(db.run(results), 5 seconds).value match {
      case Some(result) => result match {
        case Success(value) => Some(value.map(Event.tupled(_)).toList)
        case Failure(_) => None
      }
      case None => None
    }
  }

  def extractEventIds(events: List[Event]): JMap[String, Integer] = {
    events.foldLeft(Map.empty[String, Integer]) {
      case (acc, event) => acc.updated(event.title, event.eventId)
    }.asJava
  }
}
