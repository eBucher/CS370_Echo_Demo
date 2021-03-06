--
-- Schema
--

CREATE SCHEMA ssucalendar;
ALTER SCHEMA ssucalendar OWNER TO ssuadmin;

SET search_path TO ssucalendar;


--
-- Tables
--

CREATE TABLE calendar_event_ids (
  event_id smallint NOT NULL,
  event_uid text NOT NULL,
  CONSTRAINT calendar_event_ids_id PRIMARY KEY (event_id, event_uid)
);
ALTER TABLE calendar_event_ids OWNER TO ssuadmin;

-- last_updated is initialized to 'epoch' by default so that the URL is not
-- skipped the first time the scraper runs.
CREATE TABLE calendar_urls(
  url_id SMALLSERIAL NOT NULL,
  url_text text NOT NULL,
  last_updated timestamp with time zone NOT NULL DEFAULT 'epoch',
  CONSTRAINT url_id PRIMARY KEY (url_id)
);
ALTER TABLE calendar_urls OWNER TO ssuadmin;

-- `event_categories` stores a "many-to-many relationship" between events and
-- categories.  There are many categories, and each event can have more than one
-- category, so this table keeps track of which events have which categories.
CREATE TABLE event_categories(
  event_id smallint NOT NULL,
  category_id smallint NOT NULL,
  CONSTRAINT event_categories_id PRIMARY KEY (event_id, category_id)
);
ALTER TABLE event_categories OWNER TO ssuadmin;

-- The fields that are unique to every event are kept here, with a few minor
-- exceptions.
--
-- Booleans have only two possible values, so naturally values will be repeated
-- for many rows.  However, boolean values are so small that it would take more
-- space to store a table of the mappings to true/false than is needed to simply
-- store the value.  When there are multiple boolean columns in a table, the
-- values can be packed into the same byte, further saving space.
--
-- TODO: The timestamps really should be broken out into their own table.
--
-- The nullable fields are most likely to be null or different for the majority
-- of events.  They could be stored separately, but it might not be worth doing.
--
-- The summary text and description text in the calendar aren't necessarily in a
-- "speech-friendly" form, it is not practical to manually translate each event.
-- It may be desirable instead to design a translation function that can strip
-- unspeakable items (eg. HTML, URLs), and translate a few important words into
-- the proper SSML (eg. how to pronounce "Beaujolais").
CREATE TABLE events(
  event_id smallserial NOT NULL,
  title text NOT NULL, -- "title" or "name" of the event
  description text NOT NULL, -- The description is long, may contain HTML
  all_day_event boolean NOT NULL DEFAULT FALSE,
  start timestamp with time zone NOT NULL,
  "end" timestamp with time zone NOT NULL,
  event_type_id smallint NOT NULL,
  location_id smallint,
  general_admission_fee text,
  student_admission_fee text,
  open_to_public boolean,
  website_url text,
  ticket_sales_url text,
  contact_id smallint,
  CONSTRAINT event_id PRIMARY KEY (event_id),
  UNIQUE (title, start)
);
ALTER TABLE events OWNER TO ssuadmin;

-- There are about 70 or so different locations encountered for >900 events, so
-- that merits a separate table.  We may want an additional column for adding an
-- "speech-friendly" version of the values.
CREATE TABLE locations(
  location_id smallserial NOT NULL,
  name text UNIQUE NOT NULL, -- The values here can be a bit weird | see the calendar
  CONSTRAINT location_id PRIMARY KEY (location_id)
);
ALTER TABLE locations OWNER TO ssuadmin;

-- Every event has an event type in addition to categories.  There are 15
-- different types so far.
CREATE TABLE event_types(
  event_type_id smallserial NOT NULL,
  name text UNIQUE NOT NULL, -- Some contain / or ,
  CONSTRAINT event_type_id PRIMARY KEY (event_type_id)
);
ALTER TABLE event_types OWNER TO ssuadmin;

-- The contacts table is very lazy.  Contact information is optional and all
-- fields are optional.  It's gross.
CREATE TABLE contacts(
  contact_id smallserial NOT NULL,
  name text NOT NULL DEFAULT '',
  phone text NOT NULL DEFAULT '',
  email text NOT NULL DEFAULT '',
  CONSTRAINT contact_id PRIMARY KEY (contact_id),
  CONSTRAINT name_phone_email UNIQUE (name, phone, email)
);
ALTER TABLE contacts OWNER TO ssuadmin;

-- There are 9 main categories and 24 custom categories.  We just throw them all
-- together.  The category names can be very awkward, so there will likely need
-- to be an additional column for an "speech-friendly" version of these values.
CREATE TABLE categories(
  category_id smallserial NOT NULL,
  name text UNIQUE NOT NULL, -- These can have really weird values
  CONSTRAINT category_id PRIMARY KEY (category_id)
);
ALTER TABLE categories OWNER TO ssuadmin;

CREATE TABLE requests(
  request_id serial NOT NULL,
  content jsonb NOT NULL,
  CONSTRAINT request_id PRIMARY KEY (request_id)
);
ALTER TABLE requests OWNER TO ssuadmin;

CREATE TABLE sessions(
  session_id serial NOT NULL,
  content jsonb NOT NULL,
  CONSTRAINT session_id PRIMARY KEY (session_id)
);
ALTER TABLE sessions OWNER TO ssuadmin;

CREATE TABLE responses(
  response_id serial NOT NULL,
  content jsonb NOT NULL,
  CONSTRAINT response_id PRIMARY KEY (response_id)
);
ALTER TABLE responses OWNER TO ssuadmin;


--
-- Views
--

CREATE VIEW event_info AS
  SELECT e.event_id, e.title, e.start, l.name AS location FROM events e
  LEFT JOIN locations l USING (location_id)
  ORDER BY e.start ASC;
ALTER VIEW event_info OWNER TO ssuadmin;


--
-- Functions
--

-- Return all events from one category in a given time frame.
-- Note: startDay is a `date` type to intentionally truncate time info.
CREATE FUNCTION given_category(category text, startDay date, endDay date)
  RETURNS TABLE (event_id smallint, title text, start timestamp with time zone, location text) AS
  $$
  BEGIN
    IF (category = 'all') THEN
      RETURN QUERY SELECT ei.event_id, ei.title, ei.start, ei.location FROM event_info ei
        WHERE ei.start >= startDay AND ei.start < endDay;
    ELSE
      RETURN QUERY WITH cat(id) AS (
        SELECT category_id FROM categories c WHERE c.name = category
      ), ev(id) AS (
        SELECT ec.event_id FROM event_categories ec
        JOIN cat ON cat.id = ec.category_id
      )
      SELECT ei.event_id, ei.title, ei.start, ei.location FROM event_info ei
        JOIN ev on ev.id = ei.event_id
        WHERE ei.start >= startDay AND ei.start < endDay;
    END IF;
  END;
  $$
  LANGUAGE plpgsql;
ALTER FUNCTION given_category(text, date, date) OWNER TO ssuadmin;

-- Returns the number of days between now and an event with the given event_name.
-- If there are multiple days with the same name, the function will return
-- the number of days between now and the next upcoming one.
-- If the event is currently in progress, the function will get get the
-- next one.
CREATE FUNCTION days_until_event(event_name text)
  RETURNS INTEGER AS
  $days_until_event$
  DECLARE
    event_date timestamp;
    days_until_event integer;
  BEGIN
    SELECT start INTO event_date
      FROM events
      WHERE title = event_name AND start > now()
      ORDER BY start ASC
      LIMIT 1;

    SELECT event_date::date - now()::date
      INTO days_until_event;

    RETURN days_until_event;

  END;
  $days_until_event$
  LANGUAGE plpgsql;
ALTER FUNCTION days_until_event(text) OWNER TO ssuadmin;

-- returns true if:
--     The given_date is on a weekend
--     The given_date is between the start and end of an event
--     that is a 'School Holiday'
-- Otherwise, the function will return false.
CREATE FUNCTION is_school_holiday(given_date date)
  RETURNS BOOLEAN AS
  $$
  DECLARE
    is_school boolean;
    num_events integer;
    day_of_week double precision;
  BEGIN
   SELECT count(*)
     INTO num_events
     FROM events
     LEFT JOIN event_types
       ON event_types.event_type_id = events.event_type_id
     WHERE event_types.name = 'School Holiday'
       AND (start::date = given_date
            OR (start::date <= given_date AND given_date <= "end"::date));

   IF (num_events > 0) THEN
     RETURN true;
   END IF;

   day_of_week = date_part('dow', given_date);
   IF (day_of_week = 0 OR day_of_week = 6) THEN
     RETURN true;
   END IF;

   RETURN false;

  END;
  $$
  LANGUAGE plpgsql;
ALTER FUNCTION is_school_holiday(date) OWNER TO ssuadmin;


--
-- Foreign key constraints
--

-- TODO: figure out correct ON DELETE behaviors

ALTER TABLE event_categories ADD CONSTRAINT event_id FOREIGN KEY (event_id)
  REFERENCES events (event_id) MATCH FULL
  ON DELETE NO ACTION ON UPDATE NO ACTION
  INITIALLY DEFERRED;

ALTER TABLE event_categories ADD CONSTRAINT category_id FOREIGN KEY (category_id)
  REFERENCES categories (category_id) MATCH FULL
  ON DELETE NO ACTION ON UPDATE NO ACTION
  INITIALLY DEFERRED;

ALTER TABLE events ADD CONSTRAINT location_id FOREIGN KEY (location_id)
  REFERENCES locations (location_id) MATCH FULL
  ON DELETE NO ACTION ON UPDATE NO ACTION
  INITIALLY DEFERRED;

ALTER TABLE events ADD CONSTRAINT event_type_id FOREIGN KEY (event_type_id)
  REFERENCES event_types (event_type_id) MATCH FULL
  ON DELETE NO ACTION ON UPDATE NO ACTION
  INITIALLY DEFERRED;

ALTER TABLE events ADD CONSTRAINT contact_id FOREIGN KEY (contact_id)
  REFERENCES contacts (contact_id) MATCH FULL
  ON DELETE NO ACTION ON UPDATE NO ACTION
  INITIALLY DEFERRED;


--
-- Permissions
--

GRANT CREATE,USAGE
  ON SCHEMA ssucalendar
  TO ssuadmin;

GRANT USAGE
  ON SCHEMA ssucalendar
  TO alexaskill, scraper;

GRANT SELECT
  ON TABLE events, event_categories, event_info, event_types,
           contacts, categories, locations, calendar_event_ids
  TO alexaskill;

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER
  ON TABLE events, event_categories, event_info, event_types,
           contacts, categories, locations, calendar_event_ids
  TO scraper;

GRANT SELECT
  ON TABLE calendar_urls
  TO scraper;

GRANT SELECT
  ON SEQUENCE calendar_urls_url_id_seq
  TO scraper;

GRANT UPDATE (last_updated)
  ON TABLE calendar_urls
  TO scraper;

GRANT SELECT
  ON SEQUENCE events_event_id_seq, contacts_contact_id_seq,
              categories_category_id_seq, locations_location_id_seq,
              event_types_event_type_id_seq
  TO alexaskill;

GRANT USAGE,SELECT,UPDATE
  ON SEQUENCE requests_request_id_seq, sessions_session_id_seq,
              responses_response_id_seq
  TO alexaskill;

GRANT USAGE,SELECT,UPDATE
  ON SEQUENCE events_event_id_seq, contacts_contact_id_seq,
              categories_category_id_seq, locations_location_id_seq,
              event_types_event_type_id_seq
  TO scraper;

GRANT SELECT,INSERT
  ON TABLE requests, sessions, responses
  TO alexaskill;

GRANT EXECUTE
  ON FUNCTION given_category(text, date, date), days_until_event(text),
              is_school_holiday(date)
  TO alexaskill;
