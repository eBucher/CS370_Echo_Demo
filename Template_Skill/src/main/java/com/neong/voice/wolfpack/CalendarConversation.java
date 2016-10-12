package com.neong.voice.wolfpack;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.neong.voice.model.base.Conversation;
import com.wolfpack.database.DbConnection;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.ZonedDateTime;

public class CalendarConversation extends Conversation {
	// Intent names
	private final static String NEXT_EVENT_INTENT = "NextEventIntent";

	// Session attributes
	private final static String CURSOR_POSITION_ATTRIB = "cursor_position";

	// Date/time formatters
	private final static ZoneId PST = ZoneId.of("America/Los_Angeles");
	private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
	private final static DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");
	private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("????MMdd");

	// Database connection
	private DbConnection db;


	// Class constructor (note: we get constructed on every request)
	public CalendarConversation() {
		// Call the parent class constructor.
		super();

		// Create a database connection using the specified settings file.
		db = new DbConnection("DbCredentials.xml");

		// Add custom intent names for dispatcher use.
		supportedIntentNames.add(NEXT_EVENT_INTENT);
	}


	// Intent request handler (this is our main entry point)
	@Override
	public SpeechletResponse respondToIntentRequest(IntentRequest intentReq, Session session) {
		Intent intent = intentReq.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;
		SpeechletResponse response = null;

		// Get a connection to the database
		if (!db.getRemoteConnection())
			// We couldn't connect; fail gracefully
			return internalErrorResponse();

		// Dispatch to the specifc handler for the intent received
		switch (intentName) {

		case NEXT_EVENT_INTENT:
			// Initialize the position to 0 if not already set.
			if (session.getAttribute(CURSOR_POSITION_ATTRIB) == null)
				session.setAttribute(CURSOR_POSITION_ATTRIB, 0);

			response = handleNextEventIntent(intentReq, session);
			break;

		// TODO: handle cursor control intents (AMAZON.NextIntent, AMAZON.PreviousIntent)

		default:
			// This should never be reached; we only get intents we support
			return internalErrorResponse();

		}

		return response;
	}


	// Handle a specific intent (TODO: this name doesn't make sense anymore)
	private SpeechletResponse handleNextEventIntent(IntentRequest intentReq, Session session) {
		Map<String, Vector<Object>> results =
			db.runQuery("SELECT * FROM ssucalendar.event_info WHERE start > now() LIMIT 1;");
		if (results == null)
			// The query failed for some reason (check the logs)
			return internalErrorResponse();

		// State we want to persist between requests is stored in the session
		int position = (int) session.getAttribute(CURSOR_POSITION_ATTRIB);

		// TODO: this needs to be an ask response, to keep the session open
		return newTellResponse(formatResponseSsml(position, results), true);
	}


	// "Pay no attention to that man behind the curtain!"
	private static SpeechletResponse internalErrorResponse() {
		return newTellResponse("Sorry, I'm on break.", false);
	}


	// Helper to extract info from results at position and format as SSML
	private static String formatResponseSsml(int position, Map<String, Vector<Object>> results) {
		// The "summary" is the title of the event.
		String summary = (String) results.get("summary").get(position);

		Timestamp start = (Timestamp) results.get("start").get(position);

		// Location is optional on the calendars.
		String location = (String) results.get("name").get(position);
		if (location == null)
			location = "Sonoma State University";

		// Format the start timestamp into day of week, month+day, and 12-hour time strings.
		ZonedDateTime zonedDateTime = start.toLocalDateTime().atZone(PST);
		String date = zonedDateTime.format(DATE_FORMATTER);
		String day = zonedDateTime.format(DAY_FORMATTER);
		String time = zonedDateTime.format(TIME_FORMATTER);

		// Return a message decorated with SSML tags.
		return "<speak>Okay, the next event is " + summary +
			" on " + day + " <say-as interpret-as=\"date\">" + date +
			"</say-as> at <say-as interpret-as=\"time\">" + time +
			"</say-as> at " + location + ".</speak>";
	}

}
