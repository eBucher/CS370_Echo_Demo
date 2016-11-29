package com.wolfpack

import collection.JavaConverters._

import com.neong.voice.wolfpack.CalendarHelper

import java.sql.Timestamp

object CalendarDataFormatter {
  import CalendarDataSource.Event

  def formatEventSsml(format: String, event: Event): String = {
    val len = format.length
    val resultBuilder = new StringBuilder(len)
    var i = 0

    while (i < len) {
      val c = format.charAt(i)
      i = i + 1

      if (c == '{') {
        val fieldBuilder = new StringBuilder()
        var c1: Char = '*'

        // This should throw an exception if the format string is malformed.
        do {
          c1 = format.charAt(i)
          i = i + 1
          if (c1 != '}')
            fieldBuilder.append(c1)
        } while (c1 != '}')

        val field = fieldBuilder.toString
        val value = formatEventFieldSsml(field, event)

        resultBuilder.append(value)
      } else {
        resultBuilder.append(c)
      }
    }

    val result = resultBuilder.toString()

    CalendarHelper.replaceUnspeakables(result)
  }

  def formatEventSsml(format: String, events: List[Event], index: Integer): String =
    formatEventSsml(format, events(index))

  def formatEventSsml(format: String, events: List[Event]): String =
    formatEventSsml(format, events(0))

  def formatEventFieldSsml(field: String, event: Event): String = {
    field match {
      case "start:date" => CalendarHelper.formatDateSsml(event.start)
      case "start:time" => CalendarHelper.formatTimeSsml(event.start)

      case "title" => event.title

      case _ => "?"
    }
  }

  def listEvents(format: String, events: List[Event]): String = {
    val eventsLength = events.size
    val eventsList = new StringBuilder(eventsLength * format.length)

    for (i <- 0 until eventsLength) {
      if (i == eventsLength - 1 && eventsLength != 1)
        eventsList.append(" and ")
      eventsList.append(formatEventSsml(format, events, i))
    }

    eventsList.toString
  }
}
