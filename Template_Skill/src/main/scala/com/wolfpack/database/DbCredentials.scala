package com.wolfpack.database

trait DbCredentials {
  val hostname = CredentialsXml.getField("hostName")
  val port = CredentialsXml.getField("port")
  val dbname = CredentialsXml.getField("dbName")
  val username = CredentialsXml.getField("username")
  val password = CredentialsXml.getField("password")
  val sslpath = CredentialsXml.getField("localPathToSSL")
  val sslcert = getClass.getClassLoader.getResource(sslpath).getFile
  val schema = CredentialsXml.getField("schema")

  val jdriver = "com.wolfpack.database.MyPostgresDriver"
  val sdriver = "org.postgresql.Driver"
  val jdbcurl = s"jdbc:postgresql://${hostname}:${port}/${dbname}?user=${username}&password=${password}&sslmode=verify-full&sslrootcert=${sslcert}&currentSchema=${schema}"

  val outdir = "/tmp/schema"
  val outpkg = "com.wolfpack.database"

  val args = Array(
    jdriver,
    sdriver,
    jdbcurl,
    outdir,
    outpkg
  )
}

object CredentialsXml {
  val filename = "DbCredentials.xml"
  val filepath = getClass.getClassLoader.getResource(filename).getFile
  val credentialsXml = scala.xml.XML.loadFile(filepath)

  def getField(label: String): String =
    credentialsXml.child.find(_.label == label).get.child.head.toString
}

object DbCredentials extends DbCredentials
