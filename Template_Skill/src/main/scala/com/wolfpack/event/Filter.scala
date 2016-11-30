package com.wolfpack.event

import java.sql.Date

import scala.beans.BeanProperty


object FilterKind extends Enumeration {
  type FilterKind = Value

  val Category = Value("categoryFilter")
  val Start = Value("startFilter")
  val Title = Value("titleFilter")
}
import FilterKind._

abstract class Filter(@BeanProperty val kind: String)
case class CategoryFilter(@BeanProperty category: String) extends Filter(Category.toString)
case class StartFilter(@BeanProperty start: Date, @BeanProperty end: Date) extends Filter(Start.toString)
case class TitleFilter(@BeanProperty title: String) extends Filter(Title.toString)

object Filter {
  import java.util.{Map => JMap}

  def load(blob: JMap[String, Object]): Filter = {
    val kindName = blob.get("kind").asInstanceOf[String]
    FilterKind.withName(kindName) match {
      case Category =>
        val category = blob.get("category").asInstanceOf[String]
        CategoryFilter(category)

      case Start =>
        val start = Date.valueOf(blob.get("start").asInstanceOf[String])
        val end = Date.valueOf(blob.get("end").asInstanceOf[String])
        StartFilter(start, end)

      case Title =>
        val title = blob.get("title").asInstanceOf[String]
        TitleFilter(title)
    }
  }
}
