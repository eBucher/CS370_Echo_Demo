package com.wolfpack

import collection.JavaConverters._

import com.neong.voice.wolfpack.CalendarHelper

import java.sql.Timestamp

object CalendarDataFormatter {
  import CalendarDataSource.Event

  def formatEventSsml(format: String, events: List[Event], index: Int): String = {
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
        val event = events(index)
        val value = formatEventFieldSsml(field, event)

        resultBuilder.append(value)
      } else {
        resultBuilder.append(c)
      }
    }

    val result = resultBuilder.toString()

    CalendarHelper.replaceUnspeakables(result)
  }

  def formatEventSsml(format: String, events: List[Event]): String =
    formatEventSsml(format, events, 0)

  def formatEventFieldSsml(field: String, event: Event): String = {
    field match {
      case "start:date" => CalendarHelper.formatDateSsml(event.start)
      case "start:time" => CalendarHelper.formatTimeSsml(event.start)

      case "title" => event.title

      case _ => "?"
    }
  }
}
