#!/usr/bin/env python2.7

from contextlib import closing
from datetime import datetime
from functools import *
from itertools import *
import sys

from dateutil.parser import parse as parse_timestamp
import psycopg2
from psycopg2.extensions import AsIs
import requests
import vobject


# This import comes from secrets.py in the same directory as this script.
# If that file does not exist, create one using secrets.py-sample as a template.
from secrets import credentials, schema


DATABASE_CONNECT_ARGS = dict(
    sslmode = 'verify-full',
    sslrootcert = 'root.crt',
    sslcrl = '',
    options = '-c search_path={}'.format(schema),
    **credentials
)


# Normally, values passed to the database cursor get quoted and escaped
# so that arbitrary SQL cannot be injected through values.  The AsIs class
# is a special value that renders into the query unquoted when passed as a
# value to the database cursor.  The value below causes the default value
# from the database schema to be used.

DEFAULT = AsIs('DEFAULT')


def parse_last_modified_header(response):
    header = response.headers.get('last-modified')

    return None if header is None else parse_timestamp(header)


def fetch_icalendar(url, last_updated=None):
    """
    Returns a parsed iCalendar file as a vobject if the URL was able to be
    fetched and parsed, otherwise None.

    The last_updated parameter is an optional timestamp that will be used
    to skip calendars that have not been modified since the last update, if
    given.  If last_updated is None (the default) or if the URL does not
    respond with a "Last-Modified" header, the calendar will be fetched.
    When the calendar is not fetched, None is returned.
    """
    calendar = None

    # We use a streaming connection with a closing() block, so that only
    # the headers are initially fetched.  If we check the "Last-Modified"
    # header and decide we don't need the rest of the data, closing() takes
    # care of closing the connection cleanly when we return early.
    #
    # http://docs.python-requests.org/en/master/user/advanced/#body-content-workflow
    with closing(requests.get(url, stream=True)) as response:
        if response.ok:
            # Skip this calendar if it has not been modified since the last
            # time we updated it.  Proceed normally if either last_updated
            # or last_modified are not given.
            if last_updated:
                last_modified = parse_last_modified_header(response)
                if last_modified and last_modified < last_updated:
                    return None

            try:
                calendar = vobject.readOne(response.text)
            except vobject.base.ParseError as e:
                print ("Failed to parse calendar at URL: " + url)
        else:
            print("Failed to fetch URL: " + url)

    return calendar


#
# Helpers for the getters below
#

def pad(iterable, end):
    """
    Yields the sequence of elements in iterable then yields end indefinitely.

    Inspired by https://docs.python.org/2/library/itertools.html#recipes
    """
    return chain(iterable, repeat(end))


def padded(func):
    """
    Decorator that pads the result of func() with repeated None.
    """
    @wraps(func)
    def f(*args, **kwargs):
        return pad(func(*args, **kwargs), None)
    return f


def headonly(func):
    """
    Decorator that causes only the first item from the iterator returned by
    func() to be returned.
    """
    @wraps(func)
    def f(*args, **kwargs):
        return next(func(*args, **kwargs))
    return f


#
# Generic getters for basic and custom fields
#

# decorators
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
def get_custom_value(field_id, event):
    """
    Returns a generator that yields the values for the given custom field
    in the specified event.
    """
    int_field_id = int(field_id)
    if 'x-trumba-customfield' not in event.contents:
        return (None,)
    return (field.value for field in event.x_trumba_customfield_list
            if int(field.params['ID'][0]) == int_field_id)


#
# Helpers for the getters below
#

def check_bool(value):
    """
    Returns True if the value is "TRUE", False if the value is "FALSE", or
    None if the value is something else.
    """
    if value in ("TRUE", "FALSE"):
        return value == "TRUE"
    else:
        return None


def add_room_number(words, shortened):
    for word in words:
        if word.isdigit():
            return shortened + ' ' + str(word)


def gym_check(words):
    for word in words:
        if word == "Physical":
            return "Gymnasium by the swimming pools"


def check_evert(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 4:
            phrase = ' '.join(words[slice(i, i + 4)])
            if phrase == "Evert B. Person Theatre":
                return phrase


def check_ives(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 2:
            phrase = ' '.join(words[slice(i, i + 2)])
            if phrase == "Ives Hall":
                return add_room_number(words, phrase)


def check_lobo(words):
    for word in words:
        if word == "Lobo\'s":
            return word


def check_steve(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 2:
            phrase = ' '.join(words[slice(i, i + 2)])
            if phrase == "Stevenson Hall":
                return add_room_number(words, phrase)


def check_coop(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 2:
            phrase = ' '.join(words[slice(i, i + 2)])
            if phrase == "The Cooperage":
                return phrase


def check_student(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 2:
            phrase = ' '.join(words[slice(i, i + 2)])
            if phrase == "Student Center":
                return phrase


def check_GMC(words):
    numwords = len(words)
    for i in range(numwords):
        if numwords - i >= 3:
            phrase = ' '.join(words[slice(i, i + 3)])
            if phrase == "Green Music Center":
                return phrase


def is_holiday(event):
    description = get_value("description", event)
    if description is None:
        return False
    return description.find("$school holiday$") > -1


#
# Getters for fields that need additional processing
#

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
    if custom is None:
        return []
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


def get_location(event):
    """
    The location names in the calendar can be excessively long and repetitive.
    This function transforms an event's location into a speech-friendly value.
    Returns the transformed location if a location is specified, otherwise None.
    """
    location = get_value("location", event)
    if location is None:
        return None

    words = location.split()

    green_music_center = check_GMC(words)
    if green_music_center is not None:
        return green_music_center

    student_cent = check_student(words)
    if student_cent is not None:
        return student_cent

    coop = check_coop(words)
    if coop is not None:
        return coop

    stevenson_int = check_steve(words)
    if stevenson_int is not None:
        return stevenson_int

    lobo = check_lobo(words)
    if lobo is not None:
        return lobo

    ives = check_ives(words)
    if ives is not None:
        return ives

    evert = check_evert(words)
    if evert is not None:
        return evert

    gym = gym_check(words)
    if gym is not None:
        return gym


def get_event_type(event):
    event_type = get_custom_value(12, event)
    if event_type is None:
        event_type = "School Holiday" if is_holiday(event) else "Special"
    return event_type


def get_description(event):
    description = get_value("description", event)
    if description is None:
        return ""
    return description.replace("$school holiday$", "")


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
    def getter(field_name):
        return get_value(field_name, event)

    def custom_getter(field_id):
        return get_custom_value(field_id, event)

    return {
        "categories": get_categories(event),
        "all_day_event": get_all_day_event(event),
        "open_to_public": get_open_to_public(event),
        "location": get_location(event),
        "event_type": get_event_type(event),
        "description": get_description(event),

        "title": getter("summary"),
        "start": getter("dtstart"),
        "end": getter("dtend"),
        "event_uid": getter("uid"),

        "website_url": custom_getter(3109),
        "student_admission_fee": custom_getter(3111),
        "general_admission_fee": custom_getter(3124),
        "ticket_sales_url": custom_getter(13402),

        "contact_name": coalesce(custom_getter(13404), ''),
        "contact_phone": coalesce(custom_getter(13405), ''),
        "contact_email": coalesce(custom_getter(13406), ''),
    }


def get_records(calendar):
    """
    Generates a dict mapping column names to values for every event in the
    given calendar.
    """
    for event in calendar.vevent_list:
        yield get_record(event)


#
# Tests for checking if record fields exist
#

def has_contact_info(record):
    """
    Returns True if any of the contact info fields exist, otherwise False.
    """
    return (
        record.get('contact_name', None) != None
        or record.get('contact_phone', None) != None
        or record.get('contact_email', None) != None
    )


def has_location(record):
    """
    Returns true if the location field exists, otherwise False.
    """
    return record.get('location', None) != None


#
# Generic helpers for working with iterators
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

def make_values_sql(*fields):
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
            values_sql = make_values_sql(*fields)
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
    INSERT INTO contacts(name, phone, email)
        VALUES {:s}
        ON CONFLICT DO NOTHING
    """,
    'contact_name', 'contact_phone', 'contact_email'
)


insert_location = make_common_insert(
    """
    INSERT INTO locations(name)
        VALUES {:s}
        ON CONFLICT DO NOTHING
    """,
    'location'
)


insert_event_type = make_common_insert(
    """
    INSERT INTO event_types(name)
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
INSERT INTO categories(name)
    VALUES (%s)
    ON CONFLICT DO NOTHING
"""
    cursor.executemany(statement, ((c,) for c in event['categories']))


# The events table has a lot of fields.  Rather than repeating them all and
# having to make sure they're in the same order everywhere, we use one tuple
# to keep track of the fields and then generate the various bits of SQL from
# the tuple.
event_fields = (
    'all_day_event', 'open_to_public',
    'title', 'description', 'location_id',
    'start', '"end"', 'event_type_id',
    'general_admission_fee', 'student_admission_fee',
    'website_url', 'ticket_sales_url', 'contact_id'
)
event_fields_sql = ', '.join(event_fields)


@uses_values_fields(*event_fields)
@uses_statement_template("""
INSERT INTO events({event_fields:s})
    VALUES {{:s}}
    ON CONFLICT DO NOTHING;
""".format(
    event_fields = event_fields_sql
))
def insert_event(statement, cursor, event):
    # Set location_id in the event dict.  Not all events have a location,
    # so the value may be None instead.
    if has_location(event):
        cursor.execute(
            """SELECT location_id FROM locations
                   WHERE name = %(location)s;""",
            event
        )
        event['location_id'] = cursor.fetchone()
    else:
        event['location_id'] = None

    # Set event_type_id in the event dict.  This value is always present.
    cursor.execute(
        """SELECT event_type_id FROM event_types
               WHERE name = %(event_type)s;""",
        event
    )
    event['event_type_id'] = cursor.fetchone()

    # Set contact_id in the event dict.  Not all events have contact info,
    # so this value may be None instead.
    if has_contact_info(event):
        cursor.execute(
            """SELECT contact_id FROM contacts
                   WHERE (name, phone, email)
                       = (%(contact_name)s,
                          %(contact_phone)s,
                          %(contact_email)s);""",
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
WITH category(id) AS (
    SELECT category_id FROM categories
        WHERE name = %(name)s
), event(id) AS (
    SELECT event_id FROM events
        WHERE title = %(title)s AND start = %(start)s
)
INSERT INTO event_categories(event_id, category_id)
    SELECT event.id, category.id FROM event, category
    ON CONFLICT DO NOTHING;
"""
    # This is a dict comprehension that filters the event dict to keep only
    # the 'title' and 'start' keys.
    shared = {k:v for k,v in event.items() if k in ('title', 'start')}

    # This is a generator that yields a dict(title, start, name) for every
    # category in the event.  There are many categories per event, and many
    # events, so this table is building a many-to-many relationship.
    values = (
        { 'title': event['title'], 'start': event['start'], 'name': cat }
        for cat in event['categories']
    )
    # Execute for each category in the generator.
    cursor.executemany(statement, values)


def insert_event_uid(cursor, event):
    statement = \
"""
WITH event(id) AS (
    SELECT event_id FROM events
    WHERE title = %(title)s AND start = %(start)s
)
INSERT INTO calendar_event_ids(event_id, event_uid)
    SELECT event.id, %(event_uid)s FROM event
    ON CONFLICT DO NOTHING;
"""
    cursor.execute(statement, event)


def check_event_exists(cursor, event):
    statement = \
"""
SELECT event_id FROM calendar_event_ids
    WHERE event_uid = %(event_uid)s
"""
    cursor.execute(statement, event)
    results = cursor.fetchall()

    return len(results) > 0


def update_calendar_last_updated(cursor, url):
    statement = \
"""
UPDATE calendar_urls
    SET last_updated = CURRENT_TIMESTAMP
    WHERE url_id = %(url_id)s
"""
    cursor.execute(statement, dict(url_id=url.url_id))


#
# Database manipulation
#


class CalendarUrl(object):
    def __init__(self, url):
        self.url_id = url[0]
        self.url_text = url[1]
        self.last_updated = url[2]

    def __repr__(self):
        return "<CalendarUrl object (%s, %s, %s)>" % (
            self.url_id,
            self.url_text,
            self.last_updated
        )


def get_calendar_urls(cursor):
    cursor.execute("SELECT * FROM calendar_urls")
    return [CalendarUrl(url) for url in cursor.fetchall()]


def populate_database(cursor):
    # The parameter cursor is an object that allows us to execute commands
    # in a database session.
    # See http://initd.org/psycopg/docs/cursor.html

    for url in get_calendar_urls(cursor):
        # Show a + to indicate progress when run as a script.
        if __name__ == "__main__":
            sys.stdout.write('+')
            sys.stdout.flush()

        calendar = fetch_icalendar(url.url_text, url.last_updated)
        if calendar is None:
            continue

        for record in get_records(calendar):
            # Show a . to indicate progress when run as a script.
            if __name__ == "__main__":
                sys.stdout.write('.')
                sys.stdout.flush()

            # Skip existing events.
            if check_event_exists(cursor, record):
                continue

            if has_contact_info(record):
                insert_contact(cursor, record)
            if has_location(record):
                insert_location(cursor, record)
            insert_event_type(cursor, record)
            insert_categories(cursor, record)
            insert_event(cursor, record)
            insert_event_categories(cursor, record)
            insert_event_uid(cursor, record)

        update_calendar_last_updated(cursor, url)

    print("\nok")

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
