package com.neong.voice.wolfpack;

import java.util.Arrays;
import java.util.Calendar;

/**
 * This class is used for getting the specials at Lobos.
 * If you need to make and changes to the specials for the week, you can
 * change the data member variables.
 * As of right now (9/10/16), the night special never changes.
 */
class LobosMenu {

	public enum DayOrNight { DAY, NIGHT }

	/*
	 * These arrays must stay sorted for the binary search to work!
	 *
	 * Cycle two being shorter complicates combining these into one
	 * multi-dimensional array.
	 */
	private static final int[] CYCLE_ONE_WEEKS = { 39, 43, 47, 51 };
	private static final int[] CYCLE_TWO_WEEKS = { 40, 44, 48 };
	private static final int[] CYCLE_THREE_WEEKS = { 37, 41, 45, 49 };
	private static final int[] CYCLE_FOUR_WEEKS = { 38, 42, 46, 50 };

	private static final String[] DAY_SPECIALS = {
		/* Cycle 1 */
		"artichoke crab bruschetta, french dip, or classic cob",
		/* Cycle 2 */
		"olive tapenade bruschetta, prosciutto and fresh mozzarella, or an asian chicken salad",
		/* Cycle 3 */
		"roasted portobello mushroom bruschetta, a b l t sandwich, or a thai steak salad",
		/* Cycle 4 */
		"pancetta bruschetta, an italian grilled cheese sandwich, or a steak fajita salad"
	};

	private static final String NIGHT_SPECIALS = "a cheeseburger, a veggie burger, two carnitas tacos, or a quesadilla";

	private static final String NO_INFO_FOUND = "unfortunately, i was not able to find the special.";

	/**
	 * @return a string with the specials for the given date and time of day.
	 * If the function cannot find a special for the date or time of day,
	 * it will return the noInfoFound string.
	 *
	 * @param date      A java.util.Calendar representing the date in question.
	 * @param timeOfDay Used to specify whether to get the day specials or night
	 *                  specials.
	 */
	public static String getSpecialsForDate(Calendar date, DayOrNight timeOfDay) {

		int weekNum = date.get(Calendar.WEEK_OF_YEAR);
		int cycleNum = getCycleNum(weekNum);

		assert cycleNum >= 0 && cycleNum <= DAY_SPECIALS.length;

		if (cycleNum == 0)
			return NO_INFO_FOUND;

		switch (timeOfDay) {
		case DAY:
			return DAY_SPECIALS[cycleNum - 1];
		case NIGHT:
			return NIGHT_SPECIALS;
		}

		throw new AssertionError("Unknown timeOfDay " + timeOfDay);
	}



	/**
	 * @return the cycle number that corresponds with the weekNum that was passed
	 * to the function. If there is not a cycle for the given week, then
	 * the function will return 0.
	 *
	 * @param weekNum the number of a week in a year. The first week of the year may
	 *                not be a whole week.
	 */
	private static int getCycleNum(int weekNum) {
		if (Arrays.binarySearch(CYCLE_ONE_WEEKS, weekNum) >= 0)
			return 1;
		if (Arrays.binarySearch(CYCLE_TWO_WEEKS, weekNum) >= 0)
			return 2;
		if (Arrays.binarySearch(CYCLE_THREE_WEEKS, weekNum) >= 0)
			return 3;
		if (Arrays.binarySearch(CYCLE_FOUR_WEEKS, weekNum) >= 0)
			return 4;

		/* No match */
		return 0;
	}

}
