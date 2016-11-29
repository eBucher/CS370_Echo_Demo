package com.neong.voice.wolfpack;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;

import com.neong.voice.wolfpack.CalendarHelper;
import com.neong.voice.wolfpack.CosineSim;
import com.neong.voice.wolfpack.DateRange;

import com.neong.voice.model.base.Conversation;

import com.wolfpack.CalendarDataFormatter;
import com.wolfpack.CalendarDataSource;

import com.wolfpack.database.DbConnection;

import com.wolfpack.event.Filter;
import com.wolfpack.event.FilterChain;
import com.wolfpack.event.CategoryFilter;
import com.wolfpack.event.StartFilter;
import com.wolfpack.event.TitleFilter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import scala.Option;

import scala.collection.immutable.List;


public class CalendarConversation extends Conversation {
	/** Intent names */
	private enum CalendarIntent {
		AMAZON_CANCEL("AMAZON.CancelIntent"),
		AMAZON_HELP("AMAZON.HelpIntent"),
		AMAZON_NO("AMAZON.NoIntent"),
		AMAZON_STOP("AMAZON.StopIntent"),

		NEXT_EVENT("NextEventIntent"),
		GET_EVENTS_ON_DATE ("GetEventsOnDateIntent"),

		GET_END_DETAIL("GetEndDetailIntent"),
		GET_FEE_DETAIL("GetFeeDetailIntent"),
		GET_LOCATION_DETAIL("GetLocationDetailIntent"),

		ALL_CATEGORY("AllCategoryIntent"),
		ARTS_AND_ENTERTAINMENT_CATEGORY("ArtsAndEntertainmentCategoryIntent"),
		CLUBS_CATEGORY("ClubsCategoryIntent"),
		LECTURES_CATEGORY("LecturesCategoryIntent"),
		SPORTS_CATEGORY("SportsCategoryIntent");

		private final String value;
		private CalendarIntent(String value) { this.value = value; }
		@Override public String toString() { return value; }

		public static CalendarIntent valueOf(IntentRequest intentReq) {
			// Intent requests are dispatched to us by name,
			// so we always know the intent and name are non-null.
			String intentName = intentReq.getIntent().getName();

			for (CalendarIntent intent : CalendarIntent.values()) {
				if (intentName.equals(intent.value))
					return intent;
			}

			return null;
		}
	}

	/** Slot names */
	private enum CalendarSlot {
		EVENT_NAME("eventName"),
		AMAZON_DATE("date");

		private final String value;
		private CalendarSlot(String value) { this.value = value; }
		@Override public String toString() { return value; }

		public static String getRequestSlotValue(IntentRequest intentReq, CalendarSlot slot) {
			String slotName = slot.toString();
			// We always know the intent is non-null.
			Slot intentSlot = intentReq.getIntent().getSlot(slotName);
			if (intentSlot == null)
				return null;
			return intentSlot.getValue();
		}
	}

	/** Session attribute names */
	private enum CalendarAttrib {
		STATE_ID("stateId"),
		FILTER_LIST("filterList"),
		RECENTLY_SAID_EVENTS("recentlySaidEvents");

		private final String value;
		private CalendarAttrib(String value) { this.value = value; }
		@Override public String toString() { return value; }

		public static Object getSessionAttribute(Session session, CalendarAttrib attrib) {
			String attribName = attrib.toString();
			return session.getAttribute(attribName);
		}

		public static void setSessionAttribute(Session session, CalendarAttrib attrib, Object value) {
			String attribName = attrib.toString();
			session.setAttribute(attribName, value);
		}

		public static  void removeSessionAttribute(Session session, CalendarAttrib attrib) {
			String attribName = attrib.toString();
			session.removeAttribute(attribName);
		}
	}

	/** Session states */
	private enum SessionState {
		USER_HEARD_EVENTS, // The user has heard a list of events and can now ask about specific ones.
		LIST_TOO_LONG; // The list of events is too long, so the user must narrow it down somehow.

		public static SessionState valueOf(Session session) {
			String stateName = (String) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.STATE_ID);
			if (stateName == null)
				return null;
			return valueOf(stateName);
		}
	}

	// Other constants
	private final static int MAX_EVENTS = 5;


	// Private fields
	private DbConnection db;
	private ObjectMapper mapper;


	public CalendarConversation() {
		super();

		db = new DbConnection("DbCredentials.xml");
		mapper = new ObjectMapper();

		// Add custom intent names for dispatcher use.
		for (CalendarIntent intent : CalendarIntent.values())
			supportedIntentNames.add(intent.toString());
	}


	/**
	 * Try to log the intent request to the database, but fail soft.
	 */
	private void logIntentRequest(IntentRequest req) {
		try {
			String requestJson = mapper.writeValueAsString(req);
			PreparedStatement ps =
				db.prepareStatement("INSERT INTO requests(content) VALUES (?::jsonb)");
			ps.setString(1, requestJson);
			ps.execute();
		} catch (JsonGenerationException e) {
			System.out.println(e);
		} catch (JsonMappingException e) {
			System.out.println(e);
		} catch (JsonProcessingException e) {
			System.out.println(e);
		} catch (SQLException e) {
			System.out.println(e);
		}
	}


	/**
	 * Try to log the speechlet response to the database, but fail soft.
	 */
	private void logSpeechletResponse(SpeechletResponse resp) {
		try {
			String responseJson = mapper.writeValueAsString(resp);
			PreparedStatement ps =
				db.prepareStatement("INSERT INTO responses(content) VALUES (?::jsonb)");
			ps.setString(1, responseJson);
			ps.execute();
		} catch (JsonGenerationException e) {
			System.out.println(e);
		} catch (JsonMappingException e) {
			System.out.println(e);
		} catch (JsonProcessingException e) {
			System.out.println(e);
		} catch (SQLException e) {
			System.out.println(e);
		}
	}


	/**
	 * Conversation entry point
	 */
	@Override
	public SpeechletResponse respondToIntentRequest(IntentRequest intentReq, Session session) {
		SpeechletResponse response;

		if (!db.getRemoteConnection(CalendarHelper.TIME_ZONE))
			return newInternalErrorResponse();

		logIntentRequest(intentReq);

		CalendarIntent intent = CalendarIntent.valueOf(intentReq);

		switch (intent) {

		/*
		 * These intents are not sensitive to session state and can be invoked at any time.
		 */

		case AMAZON_CANCEL:
		case AMAZON_STOP:
			response = handleAmazonStopIntent(intentReq, session);
			break;

		case AMAZON_HELP:
			response = handleAmazonHelpIntent(intentReq, session);
			break;

		case AMAZON_NO:
			response = handleAmazonNoIntent(intentReq, session);
			break;

		case NEXT_EVENT:
			response = handleNextEventIntent(intentReq, session);
			break;

		case GET_EVENTS_ON_DATE:
			response = handleGetEventsOnDateIntent(intentReq, session);
			break;

		/*
		 * The rest of the intents are sensitive to the current state of the session.
		 */

		default:
			response = routeStateSensitiveIntents(intentReq, session);
			break;
		}

		logSpeechletResponse(response);

		return response;
	}


	/**
	 * Route a state-sensitive intent to the correct handler.
	 */
	private SpeechletResponse routeStateSensitiveIntents(IntentRequest intentReq, Session session) {
		SpeechletResponse response;

		SessionState state = SessionState.valueOf(session);
		if (state == null)
			return newBadStateResponse("handleStateSensitiveIntents");


		switch (state) {
		case USER_HEARD_EVENTS:
			response = routeDetailIntents(intentReq, session);
			break;

		case LIST_TOO_LONG:
			response = handleNarrowDownIntents(intentReq, session);
			break;

		default:
			throw new IllegalStateException("Unhandled SessionState value " + state);
		}

		return response;
	}


	/**
	 * Route a detail intent to the correct handler.
	 */
	private SpeechletResponse routeDetailIntents(IntentRequest intentReq, Session session) {
		SpeechletResponse response;

		CalendarIntent intent = CalendarIntent.valueOf(intentReq);

		switch (intent) {
		case GET_FEE_DETAIL:
			response = handleGetFeeDetailIntent(intentReq, session);
			break;

		case GET_LOCATION_DETAIL:
			response = handleGetLocationDetailIntent(intentReq, session);
			break;

		case GET_END_DETAIL:
			response = handleGetEndDetailIntent(intentReq, session);
			break;

		default:
			response = newBadStateResponse("handleDetailIntents");
			break;
		}

		return response;
	}


	private SpeechletResponse handleAmazonStopIntent(IntentRequest intentReq, Session session) {
		return newTellResponse("", false);
	}


	private SpeechletResponse handleAmazonHelpIntent(IntentRequest intentReq, Session session) {
		String responseSsml = "Hmm, I'm sorry you are having trouble.";
		String repromptSsml =
			"Try asking Sonoma State for what's happening tomorrow, " +
			"on a specific date, or next.";
		return newAffirmativeResponse(responseSsml, repromptSsml);
	}


	private SpeechletResponse handleAmazonNoIntent(IntentRequest intentReq, Session session) {
		String responseSsml = "Okay.";
		String repromptSsml = "What did you want instead?";
		return newAffirmativeResponse(responseSsml, repromptSsml);
	}


	private SpeechletResponse handleNextEventIntent(IntentRequest intentReq, Session session) {
		Option<List<CalendarDataSource.Event>> resultsOpt = CalendarDataSource.getNextEvent();
		if (resultsOpt.isEmpty())
			return newInternalErrorResponse();

		List<CalendarDataSource.Event> results = resultsOpt.get();

		String eventFormat = "The next event is {title}, on {start:date} at {start:time}.";
		String responseSsml = CalendarDataFormatter.formatEventSsml(eventFormat, results);
		String repromptSsml = "Is there anything you would like to know about this event?";

		Map<String, Integer> savedEvents = CalendarDataSource.extractEventIds(results);

		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS, savedEvents);
		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.STATE_ID, SessionState.USER_HEARD_EVENTS);
		CalendarAttrib.removeSessionAttribute(session, CalendarAttrib.FILTER_LIST);

		return newAffirmativeResponse(responseSsml, repromptSsml);
	}


	private SpeechletResponse handleGetEventsOnDateIntent(IntentRequest intentReq, Session session) {
		SpeechletResponse response;

		String givenDate = CalendarSlot.getRequestSlotValue(intentReq, CalendarSlot.AMAZON_DATE);
		if (givenDate == null)
			return newBadSlotResponse("date");

		@SuppressWarnings("unchecked")
		java.util.List<Filter> filters =
			(java.util.List<Filter>) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.FILTER_LIST);
		if (filters == null)
			filters = new ArrayList<Filter>();

		DateRange dateRange = new DateRange(givenDate);
		Filter startFilter = StartFilter.apply(dateRange.getBegin(),
		                                       dateRange.getEnd());
		filters.add(startFilter);

		// Select all events on the same day as the givenDate.
		Option<List<CalendarDataSource.Event>> resultsOpt =
			CalendarDataSource.getEventsWithFilters(filters);

		// If Alexa couldn't connect to the database or run the query:
		if (resultsOpt.isEmpty())
			return newInternalErrorResponse();

		List<CalendarDataSource.Event> results = resultsOpt.get();

		int numEvents = results.size();
		if (numEvents == 0) {
			String dateSsml = dateRange.getDateSsml();
			String responseSsml = "I couldn't find any events " + dateRange.getRelativeDate(true) + ".";
			String repromptSsml = "Can I help you find another event?";

			return newFailureResponse(responseSsml, repromptSsml);
		}

		if (numEvents <= MAX_EVENTS) {
			Map<String, Integer> savedEvents = CalendarDataSource.extractEventIds(results);

			CalendarAttrib.setSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS, savedEvents);
			CalendarAttrib.setSessionAttribute(session, CalendarAttrib.STATE_ID, SessionState.USER_HEARD_EVENTS);

			String responsePrefix = CalendarHelper.randomAffirmative() + ". Here are the events that I was able to find.";

			response = newEventListResponse(results, responsePrefix);
		} else { // more than MAX_EVENTS
			CalendarAttrib.setSessionAttribute(session, CalendarAttrib.STATE_ID, SessionState.LIST_TOO_LONG);

			String dateSsml = dateRange.getDateSsml();
			String responseSsml = "I was able to find " + numEvents + " different events " +
					dateRange.getRelativeDate(true) +
					". What kind of events would you like to hear about?";
			// TODO: only prompt for categories found in the list
			String repromptSsml = "Would you like to hear about sports, entertainment, " +
				"clubs, lectures, or all of the events?";

			response = newAffirmativeResponse(responseSsml, repromptSsml);
		}

		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.FILTER_LIST, filters);

		return response;
	}


	private SpeechletResponse handleNarrowDownIntents(IntentRequest intentReq, Session session) {
		String category;

		CalendarIntent intent = CalendarIntent.valueOf(intentReq);

		switch (intent) {
		case ALL_CATEGORY:
			category = "all";
			break;

		case SPORTS_CATEGORY:
			category = "Athletics";
			break;

		case ARTS_AND_ENTERTAINMENT_CATEGORY:
			category = "Arts and Entertainment";
			break;

		case LECTURES_CATEGORY:
			category = "Lectures and Films";
			break;

		case CLUBS_CATEGORY:
			category = "Club and Student Organizations";
			break;

		default:
			// TODO: Should inform the user what the categories are
			return newTellResponse("Sorry, I'm not quite sure what you meant.", false);
		}

		return handleNarrowDownIntent(intentReq, session, category);
	}


	private SpeechletResponse handleNarrowDownIntent(IntentRequest intentReq, Session session, String category) {
		@SuppressWarnings("unchecked")
		java.util.List<Filter> filters =
			(java.util.List<Filter>) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.FILTER_LIST);
		if (filters == null)
			filters = new ArrayList<Filter>();

		Filter categoryFilter = CategoryFilter.apply(category);
		filters.add(categoryFilter);

		Option<List<CalendarDataSource.Event>> resultsOpt =
			CalendarDataSource.getEventsWithFilters(filters);

		if (resultsOpt.isEmpty())
			return newInternalErrorResponse();

		List<CalendarDataSource.Event> results = resultsOpt.get();

		if (category.equals("all"))
			category = "";

		int numEvents = results.size();
		if (numEvents == 0) {
			// There will always be events for "all", or else we wouldn't be here.
			String responseSsml = "I couldn't find any " + category + " events.";

			return newTellResponse(responseSsml, false);
		}

		Map<String, Integer> savedEvents = CalendarDataSource.extractEventIds(results);

		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS, savedEvents);
		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.STATE_ID, SessionState.USER_HEARD_EVENTS);
		CalendarAttrib.setSessionAttribute(session, CalendarAttrib.FILTER_LIST, filters);

		// Format the first part of the response to indicate the category.
		String categoryPrefix = CalendarHelper.randomAffirmative() + ". Here are the " + category + " events that I was able to find.";

		return newEventListResponse(results, categoryPrefix);
	}


	private SpeechletResponse handleGetFeeDetailIntent(IntentRequest intentReq, Session session) {
		@SuppressWarnings("unchecked")
		Map<String, Integer> savedEvents =
			(HashMap<String, Integer>) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS);
		if (savedEvents == null)
			return newBadStateResponse("handleGetFeeDetailIntent");

		Set<String> savedEventNames = savedEvents.keySet();
		String eventNameSlotValue;

		if (savedEvents.size() == 1)
			eventNameSlotValue = (String) savedEventNames.toArray()[0];
		else
			eventNameSlotValue = CalendarSlot.getRequestSlotValue(intentReq, CalendarSlot.EVENT_NAME);

		if (eventNameSlotValue == null)
			return newBadSlotResponse("event");

		String eventName = CosineSim.getBestMatch(eventNameSlotValue, savedEventNames);
		Integer eventId = savedEvents.get(eventName);

		Map<String, Vector<Object>> results;

		try {
			String query =
				"SELECT title, general_admission_fee FROM events " +
				"    WHERE event_id = ?";

			PreparedStatement ps = db.prepareStatement(query);
			ps.setInt(1, eventId);

			results = DbConnection.executeStatement(ps);
		} catch (SQLException e) {
			System.out.println(e);
			return newInternalErrorResponse();
		}

		if (results.get("title").size() == 0)
			return newInternalErrorResponse();

		String eventFormat = "General admission for {title} is {general_admission_fee}.";

		String eventSsml = CalendarHelper.formatEventSsml(eventFormat, results);

		return newAffirmativeResponse(eventSsml, "Would you like any more info?");
	}


	private SpeechletResponse handleGetLocationDetailIntent(IntentRequest intentReq, Session session) {
		@SuppressWarnings("unchecked")
		Map<String, Integer> savedEvents =
			(HashMap<String, Integer>) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS);
		if (savedEvents == null)
			return newBadStateResponse("handleGetLocationDetailIntent");

		Set<String> savedEventNames = savedEvents.keySet();
		String eventNameSlotValue;

		if (savedEvents.size() == 1)
			eventNameSlotValue = (String) savedEventNames.toArray()[0];
		else
			eventNameSlotValue = CalendarSlot.getRequestSlotValue(intentReq, CalendarSlot.EVENT_NAME);

		if (eventNameSlotValue == null)
			return newBadSlotResponse("event");

		String eventName = CosineSim.getBestMatch(eventNameSlotValue, savedEventNames);
		Integer eventId = savedEvents.get(eventName);

		Map<String, Vector<Object>> results;

		try {
			String query =
				"SELECT title, location FROM event_info " +
				"    WHERE event_id = ?";

			PreparedStatement ps = db.prepareStatement(query);
			ps.setInt(1, eventId);

			results = DbConnection.executeStatement(ps);
		} catch (SQLException e) {
			System.out.println(e);
			return newInternalErrorResponse();
		}

		if (results.get("title").size() == 0)
			return newInternalErrorResponse();

		String eventFormat = "{title} is located at {location}.";

		String eventSsml = CalendarHelper.formatEventSsml(eventFormat, results);

		return newAffirmativeResponse(eventSsml, "Would you like to hear anything else?");
	}


	private SpeechletResponse handleGetEndDetailIntent(IntentRequest intentReq, Session session) {
		@SuppressWarnings("unchecked")
		Map<String, Integer> savedEvents =
			(HashMap<String, Integer>) CalendarAttrib.getSessionAttribute(session, CalendarAttrib.RECENTLY_SAID_EVENTS);
		if (savedEvents == null)
			return newBadStateResponse("handleGetEndTimeIntent");

		Set<String> savedEventNames = savedEvents.keySet();
		String eventNameSlotValue;

		if (savedEvents.size() == 1)
			eventNameSlotValue = (String) savedEventNames.toArray()[0];
		else
			eventNameSlotValue = CalendarSlot.getRequestSlotValue(intentReq, CalendarSlot.EVENT_NAME);

		if (eventNameSlotValue == null)
			return newBadSlotResponse("event");

		String eventName = CosineSim.getBestMatch(eventNameSlotValue, savedEventNames);
		Integer eventId = savedEvents.get(eventName);

		Map<String, Vector<Object>> results;

		try {
			String query =
				"SELECT title, \"end\" FROM events " +
				"    WHERE event_id = ?";

			PreparedStatement ps = db.prepareStatement(query);
			ps.setInt(1, eventId);

			results = DbConnection.executeStatement(ps);
		} catch (SQLException e) {
			System.out.println(e);
			return newInternalErrorResponse();
		}

		if (results.get("title").size() == 0)
			return newInternalErrorResponse();

		String eventFormat = "Okay, {title} ends at {end:time}.";
		String eventSsml = CalendarHelper.formatEventSsml(eventFormat, results);

		return newAffirmativeResponse(eventSsml, "Would you like to hear more?");
	}


	/**
	 * Generic response to list events for one or more days
	 *
	 * @param results   The results from a query. There must be start and title columns.
	 * @param prefix    An introductory sentence. Example - "Okay, here is what I found."
	 *                  Pass an empty string if there should not be a prefix.
	 * @return          A string with a message such as "<prefix.> On <day> is <event>
	 *                  at <time>, <event2> at <time2>... On <day2> there is...etc.
	 */
	private static SpeechletResponse newEventListResponse(List<CalendarDataSource.Event> events,
	                                                      String prefix) {
		String eventFormat = "<s>{title} at {start:time}</s>";
		String responseSsml = prefix + CalendarDataFormatter.listEventsWithDays(eventFormat, events);
		String repromptSsml = "Is there anything you would like to know about those events?";

		return newAffirmativeResponse(responseSsml, repromptSsml);
	}


	/**
	 * Generic response for when we have no information about the requested item
	 */
	private static SpeechletResponse newNoInfoResponse(String messageSsml) {
		return newFailureResponse(messageSsml, "Did you want any other information?");
	}


	/**
	 * Generic response for when we are missing a needed slot
	 */
	public static SpeechletResponse newBadSlotResponse(String slotName) {
		// FIXME: needs better messages?
		String messageSsml = "Which " + slotName + " are you interested in?";

		return newFailureResponse(messageSsml, "Did you want any other information?");
	}


	/**
	 * Generic affirmative response wrapper
	 *
	 * @param responseSsml the main message to respond with.  SSML markup is allowed, but the string
	 *                     should not include {@code <speak>...</speak>} tags.
	 * @param repromptSsml the message to speak to the user if they do not respond within the timeout
	 *                     window.  SSML markup is allowed, but the string should not include
	 *                     {@code <speak>...</speak>} tags.
	 * @return a new ask response that keeps the session open.  Both the response message and the
	 *         reprompt message get wrapped in {@code <speak>...</speak>} tags.
	 */
	private static SpeechletResponse newAffirmativeResponse(String responseSsml, String repromptSsml) {
		responseSsml = CalendarHelper.replacePartsOfNames(responseSsml);
		return newAskResponse("<speak>" + responseSsml + "</speak>", true,
		                      "<speak>" + repromptSsml + "</speak>", true);
	}


	/**
	 * Generic failure response wrapper
	 *
	 * @param responseSsml the main message to respond with.  SSML markup is allowed, but the string
	 *                     should not include {@code <speak>...</speak>} tags.
	 * @param repromptSsml the message to speak to the user if they do not respond within the timeout
	 *                     window.  SSML markup is allowed, but the string should not include
	 *                     {@code <speak>...</speak>} tags.
	 * @return a new ask response that keeps the session open and prepends "Sorry. " to the front of
	 *         the specified response message.  Both the response message and the reprompt message
	 *         get wrapped in {@code <speak>...</speak>} tags.
	 */
	public static SpeechletResponse newFailureResponse(String responseSsml, String repromptSsml) {
		return newAskResponse("<speak>Sorry. " + responseSsml + "</speak>", true,
		                      "<speak>" + repromptSsml + "</speak>", true);
	}


	/**
	 * Generic response for when we experience an internal error
	 */
	public static SpeechletResponse newInternalErrorResponse() {
		return newTellResponse("Sorry, I'm on break", false);
	}


	/**
	 * Generic response for when we don't know what's going on
	 */
	private SpeechletResponse newBadStateResponse(String msg) {
		System.out.println(msg);
		return newTellResponse("Sorry, I forgot what we were talking about.", false);
	}
}
