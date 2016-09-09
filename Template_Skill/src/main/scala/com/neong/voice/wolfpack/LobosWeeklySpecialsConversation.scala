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

    val response = conversationIntent match {

      case LobosWeeklySpecialsIntent =>
        "I will need a little more information from you. " +
        "Did you want the daily or the nightly specials?"

      case LobosWeeklySpecialsDayIntent =>
        "This week's specials are: " + LobosDataSource.daySpecials

      case LobosWeeklySpecialsNightIntent =>
        "This week the evening specials are: " + LobosDataSource.nightSpecials
    }

    Conversation.newTellResponse(response, false)
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
    val daySpecials = "Artichoke Crab Bruschetta, French Dip, and Classic Cobb"
    val nightSpecials = "Cheeseburger, Veggie Burger, Carnitas Tacos, and Quesadilla"
  }
}
