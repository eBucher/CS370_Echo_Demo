package com.neong.voice.wolfpack

import com.amazon.speech.speechlet.{
  IntentRequest,
  Session,
  SpeechletResponse
}
import com.neong.voice.model.base.Conversation

import LobosMenu.MenuItem

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
    val intent = intentReq.getIntent
    val conversationIntent = ConversationIntent.withName(intent.getName)

    val responseText = conversationIntent match {

      case LobosWeeklySpecialsIntent =>
        "I will need a little more information from you. " +
          "Did you want the daily or the nightly specials?"

      case LobosWeeklySpecialsDayIntent =>
        s"The daily specials are: ${LobosDataSource.daySpecials}. " +
          "These are available every day until 4pm."

      case LobosWeeklySpecialsNightIntent =>
        s"The nightly specials are: ${LobosDataSource.nightSpecials}. " +
          "These are available Monday through Friday 4pm until closing, " +
          "and all day on Saturday and Sunday."

      case LobosWeeklySpecialsPriceIntent =>
        val item = intent.getSlot(MenuItemSlot).getValue
        val price = LobosDataSource.itemPrice(item)
        s"The $item is $price"
    }

    val response = Conversation.newTellResponse(responseText, false)

    if (conversationIntent == LobosWeeklySpecialsIntent) {
      response.setShouldEndSession(false)
    }

    response
  }
}

object LobosWeeklySpecialsConversation {

  final val MenuItemSlot = "menu_item"

  object ConversationIntent extends Enumeration {
    type ConversationIntent = Value
    val LobosWeeklySpecialsIntent = Value("LobosWeeklySpecialsIntent")
    val LobosWeeklySpecialsDayIntent = Value("LobosWeeklySpecialsDayIntent")
    val LobosWeeklySpecialsNightIntent = Value("LobosWeeklySpecialsNightIntent")
    val LobosWeeklySpecialsPriceIntent = Value("LobosWeeklySpecialsPriceIntent")
  }

  object LobosDataSource {
    import java.util.{ TimeZone, GregorianCalendar }
    import LobosMenu.DayOrNight._

    private lazy val pst = TimeZone.getTimeZone("America/Los_Angeles")
    private lazy val date = new GregorianCalendar(pst);

    lazy val daySpecials = LobosMenu.getSpecialsForDate(date, DAY)
    lazy val nightSpecials = LobosMenu.getSpecialsForDate(date, NIGHT)

    /**
     * @return the price of a menu item.
     *
     * @param itemName the name of an item on the menu.  This string may be in speech style
     *                 (i.e. "cheese burger") or in enum style (i.e. "CHEESE_BURGER").
     */
    def itemPrice(itemName: String): String = {
      val enumName = itemName.toUpperCase().replace(' ', '_')
      val menuItem = MenuItem.valueOf(enumName)
      val price = LobosMenu.priceCheck(menuItem)
      "$" + f"$price%.2f"
    }
  }

}
