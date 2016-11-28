package com.wolfpack.database

import com.github.tminglei.slickpg._

object MyPostgresDriver extends ExPostgresDriver
                           with PgDate2Support {
  override val api = new API with DateTimeImplicits
}
