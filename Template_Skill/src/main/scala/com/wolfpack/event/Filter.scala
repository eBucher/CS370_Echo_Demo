package com.wolfpack.event

import java.sql.Date

abstract class Filter
case class CategoryFilter(category: String) extends Filter
case class StartFilter(start: Date, end: Date) extends Filter
case class TitleFilter(title: String) extends Filter
