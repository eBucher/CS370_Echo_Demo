package com.neong.voice.wolfpack;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.neong.voice.model.base.Conversation;
import com.wolfpack.database.DbConnection;

public class AcademicCalendar extends Conversation {
	//Intents
	private final static String INTENT_WHEN_IS_ACADEMIC_EVENT = "WhenIsAcademicEventIntent";

	//Slots
	private final static String SLOT_ACADEMIC_EVENT = "AcademicEvent";

	private final static Map<String, String> synonyms;

	static {
		Map<String, String> s = new HashMap<String, String>();
		s.put("the fall semester",             "The Fall Semester");
		s.put("labor day",                     "Labor Day");
		s.put("veterans day",                  "Veteran's Day");
		s.put("thanksgiving break",            "Thanksgiving Break");
		s.put("finals",                        "Finals");
		s.put("the spring semester",           "The Spring Semester");
		s.put("Martin Luther king junior day", "Martin Luther King Jr Day");
		s.put("m l k day",                     "Martin Luther King Jr Day");
		s.put("spring break",                  "Spring Break");
		s.put("Cesar Chavez day",              "Cesar Chavez Day");
		s.put("commencement",                  "Commencement");
		s.put("graduation",                    "Commencement");
		s.put("columbus day",                  "Columbus Day");
		s.put("presidents day",                "Presidents Day");
		synonyms = Collections.unmodifiableMap(s);
	}

	private DbConnection db;

	public AcademicCalendar() {
		super();

		db = new DbConnection("DbCredentials.xml");
		db.getRemoteConnection();
		db.runQuery("SET timezone='" + CalendarHelper.TIME_ZONE + "'");

		// Add custom intent names for dispatcher use.
		supportedIntentNames.add(INTENT_WHEN_IS_ACADEMIC_EVENT);
	}

	@Override
	public SpeechletResponse respondToIntentRequest(IntentRequest intentReq, Session session) {
		SpeechletResponse response;
		// Intent requests are dispatched to us by name,
		// so we always know the intent and name are non-null.
		String intentName = intentReq.getIntent().getName();

		switch (intentName) {

		case INTENT_WHEN_IS_ACADEMIC_EVENT:
			response = handleWhenIsIntent(intentReq, session);
			break;

		default:
			response = handleWhenIsIntent(intentReq, session);
			break;
		}

		return response;
	}

	private SpeechletResponse handleWhenIsIntent(IntentRequest intentReq, Session session) {
		Slot eventSlot = intentReq.getIntent().getSlot(SLOT_ACADEMIC_EVENT);
		String givenEvent;

		if (eventSlot == null || (givenEvent = eventSlot.getValue()) == null)
			return CalendarConversation.newBadSlotResponse("academic event");

		String eventName = Synonym.getSynonym(givenEvent, synonyms);

		Map<String, Vector<Object>> results;

		try {
			String query = "SELECT start, \"end\", description " +
				"FROM events " +
				"WHERE title = ? AND \"end\" > now() " +
				"ORDER BY start ASC " +
				"LIMIT 1";

			PreparedStatement ps = db.prepareStatement(query);
			ps.setString(1, eventName);

			results = DbConnection.executeStatement(ps);
		} catch (SQLException e) {
			System.out.println(e);
			return CalendarConversation.newInternalErrorResponse();
		}

		if (results.get("start").size() == 0)
			return newTellResponse("I couldn't seem to find any information about " + eventName, false);

		String startDateSsml = CalendarHelper.formatDateSsml((Timestamp) results.get("start").get(0));
		String endDateSsml = CalendarHelper.formatDateSsml((Timestamp) results.get("end").get(0));

		String fmt = startDateSsml.equals(endDateSsml) ?
			"{title} is on {start:date}. {description}" :
			"{title} begins on {start:date} and ends on {end:date}. {description}";
		String responseSsml = CalendarHelper.formatEventSsml(fmt, results);

		return newTellResponse(responseSsml, false);
	}
}
