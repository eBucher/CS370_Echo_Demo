package com.neong.voice.wolfpack

import com.amazon.speech.speechlet.{
  IntentRequest,
  Session,
  SpeechletResponse
}
import com.neong.voice.model.base.Conversation

class LobosWeeklySpecialsConversation extends Conversation {
  import com.neong.voice.wolfpack.LobosWeeklySpecialsConversation._
  import ConversationIntent._

  for (intent <- ConversationIntent.values) {
    supportedIntentNames.add(intent.toString)
  }

  override def respondToIntentRequest(
    intentReq: IntentRequest,
    session: Session
  ): SpeechletResponse = {
    val intentName = (intentReq getIntent) getName
    val conversationIntent = ConversationIntent withName intentName

    val responseText = conversationIntent match {

      case LobosWeeklySpecialsIntent =>
        "I will need a little more information from you. " +
        "Did you want the daily or the nightly specials?"

      case LobosWeeklySpecialsDayIntent =>
        "This week's specials are: " + LobosDataSource.daySpecials

      case LobosWeeklySpecialsNightIntent =>
        "This week the evening specials are: " + LobosDataSource.nightSpecials
    }

    val response = Conversation.newTellResponse(responseText, false)

    if (conversationIntent == LobosWeeklySpecialsIntent) {
      response.setShouldEndSession(false)
    }

    response
  }
}

object LobosWeeklySpecialsConversation {
  object ConversationIntent extends Enumeration {
    type ConversationIntent = Value
    val LobosWeeklySpecialsIntent = Value("LobosWeeklySpecialsIntent")
    val LobosWeeklySpecialsDayIntent = Value("LobosWeeklySpecialsDayIntent")
    val LobosWeeklySpecialsNightIntent = Value("LobosWeeklySpecialsNightIntent")
  }
  object LobosDataSource {
    import java.util.{ TimeZone, GregorianCalendar }
    import LobosMenu.DayOrNight._

    private lazy val pst = TimeZone.getTimeZone("America/Los_Angeles")
    private lazy val date = new GregorianCalendar(pst);

    lazy val daySpecials = LobosMenu.getSpecialsForDate(date, DAY)
    lazy val nightSpecials = LobosMenu.getSpecialsForDate(date, NIGHT)
  }
}
