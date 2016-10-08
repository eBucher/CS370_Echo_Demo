#!/usr/bin/env python2.7

from itertools import *
from functools import *

import requests
import vobject
import psycopg2
from psycopg2.extensions import AsIs

# This import comes from secrets.py in the same directory as this script.
# If that file does not exist, create one using secrets.py-sample as a template.
from secrets import credentials

CALENDAR_URLS = [
    #"https://25livepub.collegenet.com/calendars/Highlighted_Event.ics",
    "https://25livepub.collegenet.com/calendars/Arts_and_Entertainment.ics",
    #"https://25livepub.collegenet.com/calendars/Athletics_New.ics",
    #"https://25livepub.collegenet.com/calendars/Club_and_Student_Organizations.ics",
    #"https://25livepub.collegenet.com/calendars/Community_and_Alumni.ics",
    #"https://25livepub.collegenet.com/calendars/Diversity_Related.ics",
    #"https://25livepub.collegenet.com/calendars/Lectures_and_Films.ics",
    #"https://25livepub.collegenet.com/calendars/Student_Calendar.ics",
    #"https://25livepub.collegenet.com/calendars/Classes_and_Workshops.ics",
]

DATABASE_CONNECT_ARGS = dict(
    sslmode = 'verify-full',
    sslrootcert = 'root.crt',
    sslcrl = '',
    **credentials
)

# Normally, values passed to the database cursor get quoted and escaped
# so that arbitrary SQL cannot be injected through values.  The AsIs class
# is a special value that renders into the query unquoted when passed as a
# value to the database cursor.  The value below causes the default value
# from the database schema to be used.

DEFAULT = AsIs('DEFAULT')


def fetch_icalendar(url):
    resp = requests.get(url)
    if resp.ok:
        return vobject.readOne(resp.text)
    else:
        print("Failed to fetch URL: " + url)
        return None


#
# Helpers for the getters below
#

def pad(iterable, end):
    """
    Returns the sequence elements and then returns end indefinitely.

    Inspired by https://docs.python.org/2/library/itertools.html#recipes
    """
    return chain(iterable, repeat(end))

def padded(func):
    """
    Pads the result of func() with repeated end value.
    """
    @wraps(func)
    def f(*args, **kwargs):
        return pad(func(*args, **kwargs), None)
    return f

def headonly(func):
    """
    Returns only the first item from an iterator.
    """
    @wraps(func)
    def f(*args, **kwargs):
        return next(func(*args, **kwargs))
    return f


#
# Generic getters for basic and custom fields
#

@headonly
@padded
def get_value(key, event):
    """
    Returns a generator that yields the values for the given attribute of
    the specified event.
    """
    return (content.value for content in event.contents.get(key, ()))

@headonly
@padded
def get_custom_value(fid, event):
    """
    Returns a generator that yields the values for the given custom field
    in the specified event.
    """
    int_fid = int(fid)
    return (field.value for field in event.x_trumba_customfield_list
            if int(field.params['ID'][0]) == int_fid)


# Helper for the getters below
def check_bool(value):
    """
    Returns True if the value is "TRUE", False if the value is "FALSE", or
    None if the value is something else.
    """
    if value in ("TRUE", "FALSE"):
        return value == "TRUE"
    else:
        return None


#
# Getters for fields that need additional processing
#

def get_event_id(event):
    """
    Returns the numeric portion of the event UID, stripping the URL part.
    """
    value = get_value("uid", event)
    event_id = value.rsplit('/')[-1]
    return event_id

def get_categories(event):
    """
    Returns as a list the set of unique categories specified for an event.

    Event categories are listed in two fields: the categories field lists
    one category, and the custom categories field lists additional categories.
    This function combines those two fields into one list and removes any
    duplicates from the list.
    """
    basic = get_value("categories", event)
    custom = get_custom_value(3138, event)
    custom_list = custom.split(', ')
    return list(set(custom_list + basic))

def get_all_day_event(event):
    """
    Returns a Python boolean True or False for this field if present,
    otherwise DEFAULT.
    """
    value = get_value("x-microsoft-alldayevent", event)
    boolval = check_bool(value)
    return DEFAULT if boolval is None else boolval

def get_open_to_public(event):
    """
    Returns a Python boolean True or False for this field if present,
    otherwise None.
    """
    value = get_custom_value(12515, event)
    return check_bool(value)


# Helper for mapping None to ''
def coalesce(val, repl):
    return repl if val is None else val


#
# Functions for processing events and returning records
#

def get_record(event):
    """
    Returns a dict mapping column names to values from the given event.
    """
    # Convenience functions that supply the event to the inner functions.
    def getter(field):
        return get_value(field, event)
    def custom_getter(fid):
        return get_custom_value(fid, event)

    return {
        "event_id": get_event_id(event),
        "categories": get_categories(event),
        "all_day_event": get_all_day_event(event),
        "open_to_public": get_open_to_public(event),

        "summary": getter("summary"),
        "description": getter("description"),
        "location": getter("location"),
        "start": getter("dtstart"),
        "end": getter("dtend"),

        "event_type": custom_getter(12),
        "general_admission_fee": custom_getter(3124),
        "student_admission_fee": custom_getter(3138),
        "website_url": custom_getter(3109),
        "ticket_sales_url": custom_getter(13402),

        "contact_name": coalesce(custom_getter(13404), ''),
        "contact_phone": coalesce(custom_getter(13405), ''),
        "contact_email": coalesce(custom_getter(13406), ''),
    }

def get_records(urls):
    """
    Generates a dict mapping column names to values for every event in the
    given sequence of calendar URLs.

    Each calendar is fetched, parsed, and scraped for events.  The result
    is a flat sequence of events, not separated into different calendars.
    """
    for url in urls:
        cal = fetch_icalendar(url)
        if cal is None:
            continue
        for event in cal.vevent_list:
            record = get_record(event)
            yield record


#
# Some generic helpers for working with iterators
#

def count_iter(it):
    """
    Returns the number of items generated by an iterator.

    Does not work for infinite iterators.
    """
    return sum(1 for _ in it)

def take(n, it):
    """
    Returns a list of the first n items in an interator.

    Inspired by https://docs.python.org/2/library/itertools.html#recipes
    """
    return list(islice(it, n))


#
# Some helpers for functions using SQL
#

def make_values_sql(fields):
    """
    Returns a string listing the columns provided in fields, in the format
    required by the psycopg2 cursor.

    For example, given fields ("foo", "bar", "baz"), this function returns
    "(%(foo)s,%(bar)s,%(baz)s)".

    Special handling passes through fields starting with "currval", so that
    the current value of sequences in the database can be used (for foreign
    keys).

    While specifying the column names in the SQL statement is not required
    by psycopg2, doing so allows us to supply the actual values in a
    dictionary without having to worry about the being sequenced correctly.
    """
    return '({})'.format(
        ','.join(
            f if f.startswith('currval') else '%({})s'.format(f)
            for f in fields
        )
    )

def uses_values_fields(*fields):
    """
    Decorator for specifying field names to a statement template.

    Intented to be used in combination with `uses_statement_template()`.
    """
    def wrapper(func):
        @wraps(func)
        def f(*args, **kwargs):
            values_sql = make_values_sql(fields)
            return func(values_sql, *args, **kwargs)
        return f
    return wrapper

def uses_statement_template(statement_template):
    """
    Decorator for specifying a SQL statement template to be formatted
    with a string for the VALUES clause.

    Intented to be used in combination with `uses_values_fields()`.
    """
    def wrapper(func):
        @wraps(func)
        def f(values_sql, *args, **kwargs):
            statement = statement_template.format(values_sql)
            return func(statement, *args, **kwargs)
        return f
    return wrapper


# Helper for creating functions that match a common pattern for INSERT
def make_common_insert(statement_template, *fields):
    """
    Returns a function that takes a cursor and an event and executes the
    SQL statement resulting from the template and given fields.

    For example:

    make_common_insert(
        "INSERT INTO foo(bar,baz) VALUES {:s}",
        "bar", "baz"
    )

    returns a function like:

    insert_foo(cursor, event):
        cursor.execute(
            "INSERT INTO foo(bar,baz) VALUES (%(bar)s,%(baz)s)",
            event
        )
    """
    @uses_values_fields(*fields)
    @uses_statement_template(statement_template)
    def common_insert(statement, cursor, event):
        cursor.execute(statement, event)
    return common_insert


#
# Common pattern INSERT functions
#

insert_contact = make_common_insert(
    """
    INSERT INTO ssucalendar.contacts(name, phone, email)
        VALUES {:s}
        ON CONFLICT DO NOTHING
    """,
    'contact_name', 'contact_phone', 'contact_email'
)

insert_location = make_common_insert(
    """
    INSERT INTO ssucalendar.locations(name)
        VALUES {:s}
        ON CONFLICT DO NOTHING
    """,
    'location'
)

insert_event_type = make_common_insert(
    """
    INSERT INTO ssucalendar.event_types(name)
        VALUES {:s}
        ON CONFLICT DO NOTHING
    """,
    'event_type'
)


#
# Special INSERT functions
#

def insert_categories(cursor, event):
    statement = \
"""
INSERT INTO ssucalendar.categories(name)
    VALUES (%s)
    ON CONFLICT DO NOTHING
"""
    cursor.executemany(statement, ((c,) for c in event['categories']))

# The events table has a lot of fields.  Rather than repeating them all and
# having to make sure they're in the same order everywhere, we use one tuple
# to keep track of the fields and then generate the various bits of SQL from
# the tuple.
event_fields = (
    'event_id', 'all_day_event', 'open_to_public',
    'summary', 'description', 'location_id',
    'start', '"end"', 'event_type_id',
    'general_admission_fee', 'student_admission_fee',
    'website_url', 'ticket_sales_url', 'contact_id'
)
event_fields_sql = ', '.join(event_fields)
set_fields_sql = ', '.join(event_fields[1:])
set_values_sql = ', '.join('EXCLUDED.' + field for field in event_fields[1:])

@uses_values_fields(*event_fields)
@uses_statement_template("""
INSERT INTO ssucalendar.events({event_fields:s})
    VALUES {{:s}}
    ON CONFLICT (event_id) DO UPDATE
        SET ({set_fields:s}) = ({set_values:s});
""".format(
    event_fields = event_fields_sql,
    set_fields = set_fields_sql,
    set_values = set_values_sql
))
def insert_event(statement, cursor, event):
    # Set location_id in the event dict.  Not all events have a location, so
    # the value may be None instead.
    if event.get('location', None) is None:
        event['location_id'] = None
    else:
        cursor.execute(
            """SELECT location_id FROM ssucalendar.locations WHERE name = %(location)s;""",
            event
        )
        event['location_id'] = cursor.fetchone()

    # Set event_type_id in the event dict.  This value is always present.
    cursor.execute(
        """SELECT event_type_id FROM ssucalendar.event_types WHERE name = %(event_type)s;""",
        event
    )
    event['event_type_id'] = cursor.fetchone()

    # Set contact_id in the event dict. This value may be None if not present.
    if event.get('contact_name', None) != None \
      or event.get('contact_phone', None) != None \
      or event.get('contact_email', None) != None:
        cursor.execute(
            """SELECT contact_id FROM ssucalendar.contacts
                   WHERE (name, phone, email)
                       = (%(contact_name)s, %(contact_phone)s, %(contact_email)s);""",
            event
        )
        event['contact_id'] = cursor.fetchone()
    else:
        event['contact_id'] = None

    # "end" is a reserved word in SQL, so it has to be quoted.
    event['"end"'] = event['end']

    cursor.execute(statement, event)

def insert_event_categories(cursor, event):
    statement = \
"""
WITH cat(id) AS (
    SELECT category_id FROM ssucalendar.categories
        WHERE name = %(name)s
)
INSERT INTO ssucalendar.event_categories(event_id, category_id)
    SELECT %(event_id)s, id FROM cat
    ON CONFLICT DO NOTHING;
"""
    # This is a generator that yields a dict(event_id,name) for every
    # category in the event.  There are many categories per event, and many
    # events, so this table is building a many-to-many relationship.
    values = (
        { 'event_id': event['event_id'], 'name': cat }
        for cat in event['categories']
    )
    # Execute for each category in the generator.
    cursor.executemany(statement, values)


#
# Database manipulation
#

def populate_database(cursor):
    for record in get_records(CALENDAR_URLS):
        if record.get('contact_name', None) != None \
          or record.get('contact_phone', None) != None \
          or record.get('contact_email', None) != None:
            insert_contact(cursor, record)
        if record.get('location', None) != None:
            insert_location(cursor, record)
        insert_event_type(cursor, record)
        insert_categories(cursor, record)
        insert_event(cursor, record)
        insert_event_categories(cursor, record)


#
# AWS Lambda event handler
#

def handler(event, context):
    """
    This is the entry point of the Lambda function.
    """
    with psycopg2.connect(**DATABASE_CONNECT_ARGS) as connection:
        with connection.cursor() as cursor:
            populate_database(cursor)


#
# Test when run as a script
#

if __name__ == '__main__':
    """
    This is the entry point when run as a script.
    """
    import json

    # This is just some dummy data to simulate an actual CloudWatch event,
    # but we aren't using any of the data.
    event = json.loads(
"""{
  "account": "123456789000",
  "region": "us-east-1",
  "detail": {},
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "version": "0",
  "time": "2016-03-29T00:39:31Z",
  "id": "c714fbf3-d4ca-443d-a0f6-b1b1472d0291",
  "resources": ["arn:aws:events:us-east-1:123456789000:rule/Test"]
}"""
    )

    handler(event, {})
