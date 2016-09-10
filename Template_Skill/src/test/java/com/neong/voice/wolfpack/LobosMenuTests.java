package com.neong.voice.wolfpack;

import org.testng.annotations.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.neong.voice.wolfpack.LobosMenu;
import com.neong.voice.wolfpack.LobosMenu.DayOrNight;

public class LobosMenuTests {

	/**
	 * Test every date between the first day on the Lobos Specials calendar to the last day.
	 * None of these dates should throw an exception.
	 */
	@Test
	public void verifyKnownDateResponse() {
		// java.util.Calendar counts months from 0
		Calendar stop = new GregorianCalendar(2016, 11, 17);

		for (Calendar date = new GregorianCalendar(2016, 8, 4);
		     date.compareTo(stop) <= 0;
		     date.add(Calendar.DAY_OF_YEAR, 1)) {
			String daySpecials = LobosMenu.getSpecialsForDate(date, DayOrNight.DAY);
			String nightSpecials = LobosMenu.getSpecialsForDate(date, DayOrNight.NIGHT);
		}
	}

	/**
	 * Test some unknown dates.
	 * Unknown dates should not throw an exception.
	 */
	@Test
	public void verifyUnknownDateResponse() {
		String daySpecials = "";
		String nightSpecials = "";

		// A date in the past
		Calendar past = new GregorianCalendar(2015, 10, 20);
		daySpecials = LobosMenu.getSpecialsForDate(past, DayOrNight.DAY);
		nightSpecials = LobosMenu.getSpecialsForDate(past, DayOrNight.NIGHT);

		// A date in the future
		Calendar future = new GregorianCalendar(2200, 2, 23);
		daySpecials = LobosMenu.getSpecialsForDate(future, DayOrNight.DAY);
		nightSpecials = LobosMenu.getSpecialsForDate(future, DayOrNight.NIGHT);
	}

}
