package com.wolfpack

import collection.JavaConverters._

import com.neong.voice.wolfpack.CalendarHelper

import java.sql.Timestamp

import java.time.temporal.ChronoField

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

  def listEventsWithDays(format: String, events: List[Event]): String = {
    val eventsLength = events.size
    val eventsList = new StringBuilder(eventsLength * format.length)
    eventsList.append(s"On ${CalendarHelper.formatDateSsml(events(0).start)} there is: ")

    var currentDay = events(0).start.toLocalDateTime.get(ChronoField.EPOCH_DAY)

    for (i <- 0 until eventsLength) {
      val eventDay = events(i).start.toLocalDateTime.get(ChronoField.EPOCH_DAY)
      if (eventDay != currentDay) {
        val currentDateSsml = CalendarHelper.formatDateSsml(events(i).start)
        eventsList.append(s"""<break strength="strong"/> On ${currentDateSsml} there is: """)
      } else if (lastEventOnDay(events, i)) {
        eventsList.append("and ")
      }

      val eventSsml = formatEventSsml(format, events, i)
      eventsList.append(eventSsml)
    }

    eventsList.toString
  }

  def lastEventOnDay(events: List[Event], index: Integer): Boolean = {
    if (index == events.size - 1) { // Avoids accessing past the end of the list
      // If this is the last of all the events, then it must be the last on this day.
      true
    } else {
      val eventDay = events(index).start.toLocalDateTime.get(ChronoField.EPOCH_DAY)
      val nextDay = events(index + 1).start.toLocalDateTime.get(ChronoField.EPOCH_DAY)
      // If this day isn't the same as the next day, then this is the last on this day.
      eventDay != nextDay
    }
  }
}
