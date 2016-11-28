package com.wolfpack.database

import java.net.URI

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

import slick.backend.DatabaseConfig
import slick.codegen.SourceCodeGenerator
import slick.driver.JdbcProfile
import slick.{model => m}
import slick.util.ConfigExtensionMethods.configExtensionMethods

class MySourceCodeGenerator(model: m.Model) extends SourceCodeGenerator(model)

object MySourceCodeGenerator {

  def run(slickDriver: String, jdbcDriver: String, url: String, outputDir: String, pkg: String, user: Option[String], password: Option[String]): Unit = {
    val driver: JdbcProfile =
      Class.forName(slickDriver + "$").getField("MODULE$").get(null).asInstanceOf[JdbcProfile]
    val dbFactory = driver.api.Database
    val db = dbFactory.forURL(url, driver = jdbcDriver,
      user = user.getOrElse(null), password = password.getOrElse(null), keepAliveConnection = true)
    try {
      val m = Await.result(db.run(driver.createModel(None, true)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new SourceCodeGenerator(m).writeToFile(slickDriver,outputDir,pkg)
    } finally db.close
  }

  def run(uri: URI, outputDir: Option[String]): Unit = {
    val dc = DatabaseConfig.forURI[JdbcProfile](uri)
    val pkg = dc.config.getString("codegen.package")
    val out = outputDir.getOrElse(dc.config.getStringOr("codegen.outputDir", "."))
    val slickDriver = if(dc.driverIsObject) dc.driverName else "new " + dc.driverName
    try {
      val m = Await.result(dc.db.run(dc.driver.createModel(None, true)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new SourceCodeGenerator(m).writeToFile(slickDriver, out, pkg)
    } finally dc.db.close
  }

  def main(args: Array[String]): Unit = {
    args.toList match {
      case uri :: Nil =>
        run(new URI(uri), None)
      case uri :: outputDir :: Nil =>
        run(new URI(uri), Some(outputDir))
      case slickDriver :: jdbcDriver :: url :: outputDir :: pkg :: Nil =>
        run(slickDriver, jdbcDriver, url, outputDir, pkg, None, None)
      case slickDriver :: jdbcDriver :: url :: outputDir :: pkg :: user :: password :: Nil =>
        run(slickDriver, jdbcDriver, url, outputDir, pkg, Some(user), Some(password))
      case _ => {
        println("""
            |Usage:
            |  SourceCodeGenerator configURI [outputDir]
            |  SourceCodeGenerator slickDriver jdbcDriver url outputDir pkg [user password]
            |
            |Options:
            |  configURI: A URL pointing to a standard database config file (a fragment is
            |    resolved as a path in the config), or just a fragment used as a path in
            |    application.conf on the class path
            |  slickDriver: Fully qualified name of Slick driver class, e.g. "slick.driver.H2Driver"
            |  jdbcDriver: Fully qualified name of jdbc driver class, e.g. "org.h2.Driver"
            |  url: JDBC URL, e.g. "jdbc:postgresql://localhost/test"
            |  outputDir: Place where the package folder structure should be put
            |  pkg: Scala package the generated code should be places in
            |  user: database connection user name
            |  password: database connection password
            |
            |When using a config file, in addition to the standard config parameters from
            |slick.backend.DatabaseConfig you can set "codegen.package" and
            |"codegen.outputDir". The latter can be overridden on the command line.
          """.stripMargin.trim)
        System.exit(1)
      }
    }
  }
}
