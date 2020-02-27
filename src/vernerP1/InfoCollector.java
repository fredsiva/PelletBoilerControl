package vernerP1;

import java.util.Calendar;

public class InfoCollector {

	private static int feedTimePerDay[] = new int[356];
	private static int feedTimePerHour[] = new int[24];
	private static int numberIgnitionPerDay[] = new int[356];
	private static int 	lastHour = -1, 
						newHour, 
						lastDay = -1, 
						newDay;
	static VernerCtrl142 theController;
	int i;
	
	public InfoCollector(VernerCtrl142 aController) {	
		theController = aController;
	}
	
	public static String getHourlyStats() {
		String str;
		int i = 0;
		
		str = "Hourly Feed Time (in seconds per hour): ";
		
		while (i < 24) {
			str += "\n" + (i+1) + "h=" + feedTimePerHour[i] + "s,";
			i++;
		}
		
		return str;
	}
	
	public static void incrementIgnitionCount() {
		int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

		numberIgnitionPerDay[day] += 1; 
		
	}
	public static void  addFeedTime(int secFeed) {
		newDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		newHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		
		if (newDay != lastDay) {
			feedTimePerDay[newDay] = secFeed;	// reset
			numberIgnitionPerDay[newDay] = 0; 

			lastDay = newDay;
			if (newDay > 1) {
				theController.log("Yesterday feeding time: Day=" + (newDay-1) + " is " + feedTimePerDay[newDay-1] + "secs and " +
						numberIgnitionPerDay[newDay-1] + " ignitions", 2);
			}
		} else {
			feedTimePerDay[newDay] += secFeed;
		}
		
		if (newHour != lastHour) {
			feedTimePerHour[newHour] = secFeed;		// reset
			lastHour = newHour;
			
			theController.log("" + getHourlyStats() , 0);
			theController.log("Total Day feed = " + feedTimePerDay[newDay] + "secs and " + numberIgnitionPerDay[newDay] + " ignitions", 2);
		} else {
			feedTimePerHour[newHour] += secFeed;
		}
		
	}
	
}
