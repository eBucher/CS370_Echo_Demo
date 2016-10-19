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
	private enum CursorDirection { PREVIOUS, NEXT };

	// Intent names
	private final static String EVENT_INFO_INTENT = "EventInfoIntent";

	// Session attributes
	private final static String CURSOR_POSITION_ATTRIB = "cursor_position";
	private final static String QUERY_RESULTS_ATTRIB = "query_results";

	// Date/time formatters
	private final static ZoneId PST = ZoneId.of("America/Los_Angeles");
	private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
	private final static DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE");
	private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("????MMdd");

	// Cursor control instructions (reprompt)
	private final static String CURSOR_INSTRUCTIONS_SSML =
		"<speak>You can say next to hear the next event, " +
		"or previous to hear the previous event.</speak>";

	// Database connection
	private DbConnection db;


	// Class constructor (note: we get constructed on every request)
	public CalendarConversation() {
		// Call the parent class constructor.
		super();

		// Create a database connection using the specified settings file.
		db = new DbConnection("DbCredentials.xml");

		// Add custom intent names for dispatcher use.
		supportedIntentNames.add(EVENT_INFO_INTENT);
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

		case EVENT_INFO_INTENT:
			response = handleEventInfoIntent(intentReq, session);
			break;

		case "AMAZON.NextIntent":
			response = handleCursorChangeIntent(CursorDirection.NEXT, intentReq, session);
			break;

		case "AMAZON.PreviousIntent":
			response = handleCursorChangeIntent(CursorDirection.PREVIOUS, intentReq, session);
			break;

		// TODO: how to stop/cancel

		default:
			// This should never be reached; we only get intents we support
			return internalErrorResponse();

		}

		return response;
	}


	/** Handler for EventInfoIntent requests
	 *
	 * This is the intent that initiates a listing of events.  When this
	 * intent is triggered, we use the information available to us through
	 * slots or other available context to perform a query selecting a list
	 * of events related to the user's request.  This list is stored in the
	 * session object, along with a cursor position (list index), which we
	 * initialize to zero.  Finally, a tell response is returned, reading
	 * the first block of event information and leaving the session open
	 * for further navigation.
	 *
	 * @param intentReq is the current request object from AVS.
	 * @param session is the current session object from AVS.
	 *
	 * @returns speech for the first found event, or error speech if failed.
	 */
	private SpeechletResponse handleEventInfoIntent(IntentRequest intentReq, Session session) {
		int position = 0;
		session.setAttribute(CURSOR_POSITION_ATTRIB, position);

		Map<String, Vector<Object>> results = queryEventInfo();

		if (results == null)
			// The query failed for some reason (check the logs)
			return internalErrorResponse();

		session.setAttribute(QUERY_RESULTS_ATTRIB, results);

		return newEventInfoResponse(position, results);
	}


	/** Handler for AMAZON.NextIntent and AMAZON.PreviousIntent
	 */
	private SpeechletResponse handleCursorChangeIntent(CursorDirection direction, IntentRequest intentReq, Session session) {
		int position = (int) session.getAttribute(CURSOR_POSITION_ATTRIB);
		int newPosition;

		switch (direction) {
		case NEXT:
			newPosition = position + 1;
			break;
		case PREVIOUS:
			newPosition = position - 1;
			break;
		default:
			// Never reached
			return internalErrorResponse();
		}

		session.setAttribute(CURSOR_POSITION_ATTRIB, newPosition);

		Map<String, Vector<Object>> results =
			(HashMap<String, Vector<Object>>) session.getAttribute(QUERY_RESULTS_ATTRIB);

		// TODO: bounds check newPosition for results, handle sliding window of results, etc.

		return newEventInfoResponse(newPosition, results);
	}


	// "Pay no attention to that man behind the curtain!"
	private static SpeechletResponse internalErrorResponse() {
		return newTellResponse("Sorry, I'm on break.", false);
	}


	private static SpeechletResponse newEventInfoResponse(int position, Map<String, Vector<Object>> results) {
		String responseSsml = formatResponseSsml(position, results);
		SpeechletResponse response = newAskResponse(responseSsml, true, CURSOR_INSTRUCTIONS_SSML, true);
		return response;
	}


	private Map<String, Vector<Object>> queryEventInfo() {
		return db.runQuery("SELECT * FROM ssucalendar.event_info WHERE start > now() LIMIT 5;");
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
