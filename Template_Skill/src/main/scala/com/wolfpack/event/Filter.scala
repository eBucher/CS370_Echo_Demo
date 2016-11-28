package com.wolfpack.event

import java.sql.Timestamp

abstract class Filter
case class CategoryFilter(category: String) extends Filter
case class StartFilter(start: Timestamp, end: Timestamp) extends Filter
case class TitleFilter(title: String) extends Filter
