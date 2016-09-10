package extras;

/*
 * This class is used for getting the specials at Lobos.
 * If you need to make and changes to the specials for the week, you can
 * change the data member variables.
 * As of right now (9/10/16), the night special never changes.
 */
public class LobosMenu {
	
	private static int[] cycle1Weeks = {39, 43, 47, 51};
	private static int[] cycle2Weeks = {40, 44, 48};
	private static int[] cycle3Weeks = {37, 41, 45, 49};
	private static int[] cycle4Weeks = {38,42, 46, 50};
	
	private static String cycle1DaySpecials = "artichoke crab bruschetta, french dip, or classic cob";
	private static String cycle2DaySpecials = "olive tapenade bruschetta, prosciutto and fresh mozzarella, or an asian chicken salad";
	private static String cycle3DaySpecials = "roasted portobello mushroom bruschetta, a b l t sandwich, or a thai steak salad";
	private static String cycle4DaySpecials = "pancetta bruschetta, an italian grilled cheese sandwich, or a steak fajita salad";
	
	private static String nightSpecials = "a cheeseburger, a veggie burger, two cartias tacos, or a quesadilla";
	
	private static String noInfoFound = "unfortunately, i was not able to find the special.";
	
	
	/*
	 * Returns a string with the specials for the given week and time of day.
	 * If the function cannot find a special for the week or time of day,
	 * it will return the noInfoFound string.
	 * 
	 * @param weekNum   The number of a week in a year. The first week of the year may
	 *                  not be a whole week.
	 * @param timeOfDay Used to specify whether to get the day specials or night
	 *                  specials. It must either be "day" or "night". Anything
	 *                  else will cause the function to not work properly.
	 */
	public static String getDaySpecialsForWeek(int weekNum, String timeOfDay){
		
		int cycleNum = getCycleNum(weekNum);
		
		if(timeOfDay == "night"){
			return nightSpecials;
		}
		if(timeOfDay == "day"){
			if(cycleNum == 1){
				return cycle1DaySpecials;
			}
			if(cycleNum == 2){
				return cycle2DaySpecials;
			}
			if(cycleNum == 3){
				return cycle3DaySpecials;
			}
			if(cycleNum == 4){
				return cycle4DaySpecials;
			}
		}
		
		return noInfoFound;
			
	}
	
	
	
	/*
	 * Returns the cycle number that corresponds with the weekNum that was passed
	 * to the function. If there is not a cycle for the given week, then the
	 * function will return 0.
	 * 
	 * @param weekNum The number of a week in a year. The first week of the year may
	 *                not be a whole week.
	 */
	private static int getCycleNum(int weekNum){
		for(int i = 0; i < cycle1Weeks.length; i++){
			if(cycle1Weeks[i] == weekNum){
				return 1;
			}
		}
		for(int i = 0; i < cycle2Weeks.length; i++){
			if(cycle2Weeks[i] == weekNum){
				return 2;
			}
		}
		for(int i = 0; i < cycle3Weeks.length; i++){
			if(cycle3Weeks[i] == weekNum){
				return 3;
			}
		}
		for(int i = 0; i < cycle4Weeks.length; i++){
			if(cycle4Weeks[i] == weekNum){
				return 4;
			}
		}
		
		return 0;
	}
	
}
