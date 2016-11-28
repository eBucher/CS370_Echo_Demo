package com.wolfpack.database
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = com.wolfpack.database.MyPostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(CalendarEventIds.schema, CalendarUrls.schema, Categories.schema, Contacts.schema, EventCategories.schema, Events.schema, EventTypes.schema, Locations.schema, Requests.schema, Responses.schema, Sessions.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table CalendarEventIds
   *  @param eventId Database column event_id SqlType(int2)
   *  @param eventUid Database column event_uid SqlType(text) */
  case class CalendarEventIdsRow(eventId: Short, eventUid: String)
  /** GetResult implicit for fetching CalendarEventIdsRow objects using plain SQL queries */
  implicit def GetResultCalendarEventIdsRow(implicit e0: GR[Short], e1: GR[String]): GR[CalendarEventIdsRow] = GR{
    prs => import prs._
    CalendarEventIdsRow.tupled((<<[Short], <<[String]))
  }
  /** Table description of table calendar_event_ids. Objects of this class serve as prototypes for rows in queries. */
  class CalendarEventIds(_tableTag: Tag) extends Table[CalendarEventIdsRow](_tableTag, Some("ssucalendar"), "calendar_event_ids") {
    def * = (eventId, eventUid) <> (CalendarEventIdsRow.tupled, CalendarEventIdsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(eventId), Rep.Some(eventUid)).shaped.<>({r=>import r._; _1.map(_=> CalendarEventIdsRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_id SqlType(int2) */
    val eventId: Rep[Short] = column[Short]("event_id")
    /** Database column event_uid SqlType(text) */
    val eventUid: Rep[String] = column[String]("event_uid")

    /** Primary key of CalendarEventIds (database name calendar_event_ids_id) */
    val pk = primaryKey("calendar_event_ids_id", (eventId, eventUid))
  }
  /** Collection-like TableQuery object for table CalendarEventIds */
  lazy val CalendarEventIds = new TableQuery(tag => new CalendarEventIds(tag))

  /** Entity class storing rows of table CalendarUrls
   *  @param urlId Database column url_id SqlType(int2), AutoInc, PrimaryKey
   *  @param urlText Database column url_text SqlType(text)
   *  @param lastUpdated Database column last_updated SqlType(timestamptz) */
  case class CalendarUrlsRow(urlId: Short, urlText: String, lastUpdated: java.sql.Timestamp)
  /** GetResult implicit for fetching CalendarUrlsRow objects using plain SQL queries */
  implicit def GetResultCalendarUrlsRow(implicit e0: GR[Short], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[CalendarUrlsRow] = GR{
    prs => import prs._
    CalendarUrlsRow.tupled((<<[Short], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table calendar_urls. Objects of this class serve as prototypes for rows in queries. */
  class CalendarUrls(_tableTag: Tag) extends Table[CalendarUrlsRow](_tableTag, Some("ssucalendar"), "calendar_urls") {
    def * = (urlId, urlText, lastUpdated) <> (CalendarUrlsRow.tupled, CalendarUrlsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(urlId), Rep.Some(urlText), Rep.Some(lastUpdated)).shaped.<>({r=>import r._; _1.map(_=> CalendarUrlsRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column url_id SqlType(int2), AutoInc, PrimaryKey */
    val urlId: Rep[Short] = column[Short]("url_id", O.AutoInc, O.PrimaryKey)
    /** Database column url_text SqlType(text) */
    val urlText: Rep[String] = column[String]("url_text")
    /** Database column last_updated SqlType(timestamptz) */
    val lastUpdated: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("last_updated")
  }
  /** Collection-like TableQuery object for table CalendarUrls */
  lazy val CalendarUrls = new TableQuery(tag => new CalendarUrls(tag))

  /** Entity class storing rows of table Categories
   *  @param categoryId Database column category_id SqlType(int2), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(text) */
  case class CategoriesRow(categoryId: Short, name: String)
  /** GetResult implicit for fetching CategoriesRow objects using plain SQL queries */
  implicit def GetResultCategoriesRow(implicit e0: GR[Short], e1: GR[String]): GR[CategoriesRow] = GR{
    prs => import prs._
    CategoriesRow.tupled((<<[Short], <<[String]))
  }
  /** Table description of table categories. Objects of this class serve as prototypes for rows in queries. */
  class Categories(_tableTag: Tag) extends Table[CategoriesRow](_tableTag, Some("ssucalendar"), "categories") {
    def * = (categoryId, name) <> (CategoriesRow.tupled, CategoriesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(categoryId), Rep.Some(name)).shaped.<>({r=>import r._; _1.map(_=> CategoriesRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column category_id SqlType(int2), AutoInc, PrimaryKey */
    val categoryId: Rep[Short] = column[Short]("category_id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")

    /** Uniqueness Index over (name) (database name categories_name_key) */
    val index1 = index("categories_name_key", name, unique=true)
  }
  /** Collection-like TableQuery object for table Categories */
  lazy val Categories = new TableQuery(tag => new Categories(tag))

  /** Entity class storing rows of table Contacts
   *  @param contactId Database column contact_id SqlType(int2), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(text)
   *  @param phone Database column phone SqlType(text)
   *  @param email Database column email SqlType(text) */
  case class ContactsRow(contactId: Short, name: String, phone: String, email: String)
  /** GetResult implicit for fetching ContactsRow objects using plain SQL queries */
  implicit def GetResultContactsRow(implicit e0: GR[Short], e1: GR[String]): GR[ContactsRow] = GR{
    prs => import prs._
    ContactsRow.tupled((<<[Short], <<[String], <<[String], <<[String]))
  }
  /** Table description of table contacts. Objects of this class serve as prototypes for rows in queries. */
  class Contacts(_tableTag: Tag) extends Table[ContactsRow](_tableTag, Some("ssucalendar"), "contacts") {
    def * = (contactId, name, phone, email) <> (ContactsRow.tupled, ContactsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(contactId), Rep.Some(name), Rep.Some(phone), Rep.Some(email)).shaped.<>({r=>import r._; _1.map(_=> ContactsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column contact_id SqlType(int2), AutoInc, PrimaryKey */
    val contactId: Rep[Short] = column[Short]("contact_id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column phone SqlType(text) */
    val phone: Rep[String] = column[String]("phone")
    /** Database column email SqlType(text) */
    val email: Rep[String] = column[String]("email")

    /** Uniqueness Index over (name,phone,email) (database name name_phone_email) */
    val index1 = index("name_phone_email", (name, phone, email), unique=true)
  }
  /** Collection-like TableQuery object for table Contacts */
  lazy val Contacts = new TableQuery(tag => new Contacts(tag))

  /** Entity class storing rows of table EventCategories
   *  @param eventId Database column event_id SqlType(int2)
   *  @param categoryId Database column category_id SqlType(int2) */
  case class EventCategoriesRow(eventId: Short, categoryId: Short)
  /** GetResult implicit for fetching EventCategoriesRow objects using plain SQL queries */
  implicit def GetResultEventCategoriesRow(implicit e0: GR[Short]): GR[EventCategoriesRow] = GR{
    prs => import prs._
    EventCategoriesRow.tupled((<<[Short], <<[Short]))
  }
  /** Table description of table event_categories. Objects of this class serve as prototypes for rows in queries. */
  class EventCategories(_tableTag: Tag) extends Table[EventCategoriesRow](_tableTag, Some("ssucalendar"), "event_categories") {
    def * = (eventId, categoryId) <> (EventCategoriesRow.tupled, EventCategoriesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(eventId), Rep.Some(categoryId)).shaped.<>({r=>import r._; _1.map(_=> EventCategoriesRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_id SqlType(int2) */
    val eventId: Rep[Short] = column[Short]("event_id")
    /** Database column category_id SqlType(int2) */
    val categoryId: Rep[Short] = column[Short]("category_id")

    /** Primary key of EventCategories (database name event_categories_id) */
    val pk = primaryKey("event_categories_id", (eventId, categoryId))

    /** Foreign key referencing Categories (database name category_id) */
    lazy val categoriesFk = foreignKey("category_id", categoryId, Categories)(r => r.categoryId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing Events (database name event_id) */
    lazy val eventsFk = foreignKey("event_id", eventId, Events)(r => r.eventId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table EventCategories */
  lazy val EventCategories = new TableQuery(tag => new EventCategories(tag))

  /** Entity class storing rows of table Events
   *  @param eventId Database column event_id SqlType(int2), AutoInc, PrimaryKey
   *  @param title Database column title SqlType(text)
   *  @param description Database column description SqlType(text)
   *  @param allDayEvent Database column all_day_event SqlType(bool), Default(false)
   *  @param start Database column start SqlType(timestamptz)
   *  @param end Database column end SqlType(timestamptz)
   *  @param eventTypeId Database column event_type_id SqlType(int2)
   *  @param locationId Database column location_id SqlType(int2), Default(None)
   *  @param generalAdmissionFee Database column general_admission_fee SqlType(text), Default(None)
   *  @param studentAdmissionFee Database column student_admission_fee SqlType(text), Default(None)
   *  @param openToPublic Database column open_to_public SqlType(bool), Default(None)
   *  @param websiteUrl Database column website_url SqlType(text), Default(None)
   *  @param ticketSalesUrl Database column ticket_sales_url SqlType(text), Default(None)
   *  @param contactId Database column contact_id SqlType(int2), Default(None) */
  case class EventsRow(eventId: Short, title: String, description: String, allDayEvent: Boolean = false, start: java.sql.Timestamp, end: java.sql.Timestamp, eventTypeId: Short, locationId: Option[Short] = None, generalAdmissionFee: Option[String] = None, studentAdmissionFee: Option[String] = None, openToPublic: Option[Boolean] = None, websiteUrl: Option[String] = None, ticketSalesUrl: Option[String] = None, contactId: Option[Short] = None)
  /** GetResult implicit for fetching EventsRow objects using plain SQL queries */
  implicit def GetResultEventsRow(implicit e0: GR[Short], e1: GR[String], e2: GR[Boolean], e3: GR[java.sql.Timestamp], e4: GR[Option[Short]], e5: GR[Option[String]], e6: GR[Option[Boolean]]): GR[EventsRow] = GR{
    prs => import prs._
    EventsRow.tupled((<<[Short], <<[String], <<[String], <<[Boolean], <<[java.sql.Timestamp], <<[java.sql.Timestamp], <<[Short], <<?[Short], <<?[String], <<?[String], <<?[Boolean], <<?[String], <<?[String], <<?[Short]))
  }
  /** Table description of table events. Objects of this class serve as prototypes for rows in queries. */
  class Events(_tableTag: Tag) extends Table[EventsRow](_tableTag, Some("ssucalendar"), "events") {
    def * = (eventId, title, description, allDayEvent, start, end, eventTypeId, locationId, generalAdmissionFee, studentAdmissionFee, openToPublic, websiteUrl, ticketSalesUrl, contactId) <> (EventsRow.tupled, EventsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(eventId), Rep.Some(title), Rep.Some(description), Rep.Some(allDayEvent), Rep.Some(start), Rep.Some(end), Rep.Some(eventTypeId), locationId, generalAdmissionFee, studentAdmissionFee, openToPublic, websiteUrl, ticketSalesUrl, contactId).shaped.<>({r=>import r._; _1.map(_=> EventsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9, _10, _11, _12, _13, _14)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_id SqlType(int2), AutoInc, PrimaryKey */
    val eventId: Rep[Short] = column[Short]("event_id", O.AutoInc, O.PrimaryKey)
    /** Database column title SqlType(text) */
    val title: Rep[String] = column[String]("title")
    /** Database column description SqlType(text) */
    val description: Rep[String] = column[String]("description")
    /** Database column all_day_event SqlType(bool), Default(false) */
    val allDayEvent: Rep[Boolean] = column[Boolean]("all_day_event", O.Default(false))
    /** Database column start SqlType(timestamptz) */
    val start: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("start")
    /** Database column end SqlType(timestamptz) */
    val end: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("end")
    /** Database column event_type_id SqlType(int2) */
    val eventTypeId: Rep[Short] = column[Short]("event_type_id")
    /** Database column location_id SqlType(int2), Default(None) */
    val locationId: Rep[Option[Short]] = column[Option[Short]]("location_id", O.Default(None))
    /** Database column general_admission_fee SqlType(text), Default(None) */
    val generalAdmissionFee: Rep[Option[String]] = column[Option[String]]("general_admission_fee", O.Default(None))
    /** Database column student_admission_fee SqlType(text), Default(None) */
    val studentAdmissionFee: Rep[Option[String]] = column[Option[String]]("student_admission_fee", O.Default(None))
    /** Database column open_to_public SqlType(bool), Default(None) */
    val openToPublic: Rep[Option[Boolean]] = column[Option[Boolean]]("open_to_public", O.Default(None))
    /** Database column website_url SqlType(text), Default(None) */
    val websiteUrl: Rep[Option[String]] = column[Option[String]]("website_url", O.Default(None))
    /** Database column ticket_sales_url SqlType(text), Default(None) */
    val ticketSalesUrl: Rep[Option[String]] = column[Option[String]]("ticket_sales_url", O.Default(None))
    /** Database column contact_id SqlType(int2), Default(None) */
    val contactId: Rep[Option[Short]] = column[Option[Short]]("contact_id", O.Default(None))

    /** Foreign key referencing Contacts (database name contact_id) */
    lazy val contactsFk = foreignKey("contact_id", contactId, Contacts)(r => Rep.Some(r.contactId), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing EventTypes (database name event_type_id) */
    lazy val eventTypesFk = foreignKey("event_type_id", eventTypeId, EventTypes)(r => r.eventTypeId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing Locations (database name location_id) */
    lazy val locationsFk = foreignKey("location_id", locationId, Locations)(r => Rep.Some(r.locationId), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    /** Uniqueness Index over (title,start) (database name events_title_start_key) */
    val index1 = index("events_title_start_key", (title, start), unique=true)
  }
  /** Collection-like TableQuery object for table Events */
  lazy val Events = new TableQuery(tag => new Events(tag))

  /** Entity class storing rows of table EventTypes
   *  @param eventTypeId Database column event_type_id SqlType(int2), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(text) */
  case class EventTypesRow(eventTypeId: Short, name: String)
  /** GetResult implicit for fetching EventTypesRow objects using plain SQL queries */
  implicit def GetResultEventTypesRow(implicit e0: GR[Short], e1: GR[String]): GR[EventTypesRow] = GR{
    prs => import prs._
    EventTypesRow.tupled((<<[Short], <<[String]))
  }
  /** Table description of table event_types. Objects of this class serve as prototypes for rows in queries. */
  class EventTypes(_tableTag: Tag) extends Table[EventTypesRow](_tableTag, Some("ssucalendar"), "event_types") {
    def * = (eventTypeId, name) <> (EventTypesRow.tupled, EventTypesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(eventTypeId), Rep.Some(name)).shaped.<>({r=>import r._; _1.map(_=> EventTypesRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_type_id SqlType(int2), AutoInc, PrimaryKey */
    val eventTypeId: Rep[Short] = column[Short]("event_type_id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")

    /** Uniqueness Index over (name) (database name event_types_name_key) */
    val index1 = index("event_types_name_key", name, unique=true)
  }
  /** Collection-like TableQuery object for table EventTypes */
  lazy val EventTypes = new TableQuery(tag => new EventTypes(tag))

  /** Entity class storing rows of table Locations
   *  @param locationId Database column location_id SqlType(int2), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(text) */
  case class LocationsRow(locationId: Short, name: String)
  /** GetResult implicit for fetching LocationsRow objects using plain SQL queries */
  implicit def GetResultLocationsRow(implicit e0: GR[Short], e1: GR[String]): GR[LocationsRow] = GR{
    prs => import prs._
    LocationsRow.tupled((<<[Short], <<[String]))
  }
  /** Table description of table locations. Objects of this class serve as prototypes for rows in queries. */
  class Locations(_tableTag: Tag) extends Table[LocationsRow](_tableTag, Some("ssucalendar"), "locations") {
    def * = (locationId, name) <> (LocationsRow.tupled, LocationsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(locationId), Rep.Some(name)).shaped.<>({r=>import r._; _1.map(_=> LocationsRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column location_id SqlType(int2), AutoInc, PrimaryKey */
    val locationId: Rep[Short] = column[Short]("location_id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")

    /** Uniqueness Index over (name) (database name locations_name_key) */
    val index1 = index("locations_name_key", name, unique=true)
  }
  /** Collection-like TableQuery object for table Locations */
  lazy val Locations = new TableQuery(tag => new Locations(tag))

  /** Entity class storing rows of table Requests
   *  @param requestId Database column request_id SqlType(serial), AutoInc, PrimaryKey
   *  @param content Database column content SqlType(jsonb), Length(2147483647,false) */
  case class RequestsRow(requestId: Int, content: String)
  /** GetResult implicit for fetching RequestsRow objects using plain SQL queries */
  implicit def GetResultRequestsRow(implicit e0: GR[Int], e1: GR[String]): GR[RequestsRow] = GR{
    prs => import prs._
    RequestsRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table requests. Objects of this class serve as prototypes for rows in queries. */
  class Requests(_tableTag: Tag) extends Table[RequestsRow](_tableTag, Some("ssucalendar"), "requests") {
    def * = (requestId, content) <> (RequestsRow.tupled, RequestsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(requestId), Rep.Some(content)).shaped.<>({r=>import r._; _1.map(_=> RequestsRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column request_id SqlType(serial), AutoInc, PrimaryKey */
    val requestId: Rep[Int] = column[Int]("request_id", O.AutoInc, O.PrimaryKey)
    /** Database column content SqlType(jsonb), Length(2147483647,false) */
    val content: Rep[String] = column[String]("content", O.Length(2147483647,varying=false))
  }
  /** Collection-like TableQuery object for table Requests */
  lazy val Requests = new TableQuery(tag => new Requests(tag))

  /** Entity class storing rows of table Responses
   *  @param responseId Database column response_id SqlType(serial), AutoInc, PrimaryKey
   *  @param content Database column content SqlType(jsonb), Length(2147483647,false) */
  case class ResponsesRow(responseId: Int, content: String)
  /** GetResult implicit for fetching ResponsesRow objects using plain SQL queries */
  implicit def GetResultResponsesRow(implicit e0: GR[Int], e1: GR[String]): GR[ResponsesRow] = GR{
    prs => import prs._
    ResponsesRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table responses. Objects of this class serve as prototypes for rows in queries. */
  class Responses(_tableTag: Tag) extends Table[ResponsesRow](_tableTag, Some("ssucalendar"), "responses") {
    def * = (responseId, content) <> (ResponsesRow.tupled, ResponsesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(responseId), Rep.Some(content)).shaped.<>({r=>import r._; _1.map(_=> ResponsesRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column response_id SqlType(serial), AutoInc, PrimaryKey */
    val responseId: Rep[Int] = column[Int]("response_id", O.AutoInc, O.PrimaryKey)
    /** Database column content SqlType(jsonb), Length(2147483647,false) */
    val content: Rep[String] = column[String]("content", O.Length(2147483647,varying=false))
  }
  /** Collection-like TableQuery object for table Responses */
  lazy val Responses = new TableQuery(tag => new Responses(tag))

  /** Entity class storing rows of table Sessions
   *  @param sessionId Database column session_id SqlType(serial), AutoInc, PrimaryKey
   *  @param content Database column content SqlType(jsonb), Length(2147483647,false) */
  case class SessionsRow(sessionId: Int, content: String)
  /** GetResult implicit for fetching SessionsRow objects using plain SQL queries */
  implicit def GetResultSessionsRow(implicit e0: GR[Int], e1: GR[String]): GR[SessionsRow] = GR{
    prs => import prs._
    SessionsRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table sessions. Objects of this class serve as prototypes for rows in queries. */
  class Sessions(_tableTag: Tag) extends Table[SessionsRow](_tableTag, Some("ssucalendar"), "sessions") {
    def * = (sessionId, content) <> (SessionsRow.tupled, SessionsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(sessionId), Rep.Some(content)).shaped.<>({r=>import r._; _1.map(_=> SessionsRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column session_id SqlType(serial), AutoInc, PrimaryKey */
    val sessionId: Rep[Int] = column[Int]("session_id", O.AutoInc, O.PrimaryKey)
    /** Database column content SqlType(jsonb), Length(2147483647,false) */
    val content: Rep[String] = column[String]("content", O.Length(2147483647,varying=false))
  }
  /** Collection-like TableQuery object for table Sessions */
  lazy val Sessions = new TableQuery(tag => new Sessions(tag))
}
