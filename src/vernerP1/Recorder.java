package vernerP1;

import java.util.Calendar;

class Recorder {
	private int activationTimePerDay[] = new int[366];	// Total seconds the device has been running during each day
	private int activationTimePerHour[] = new int[24];	// Total seconds the device has been running during every hour of today
	private int numberActivationsPerDay[] = new int[366];
	private int lastDay = -1, 
				startDay;
	private long startTime = 0;
	private int startHour, startMinute;
	
	public Recorder() {
		startTime = System.currentTimeMillis();		// in case Off() is called before On()			
	}
	
	public void resetRecord() {
		startTime = System.currentTimeMillis();		// in case Off() is called before On()						
	}

	public int getActivationTime(int h) {
		return activationTimePerHour[h];
	}

	public int getDailyActivationTime(int d) {
		return activationTimePerDay[d];
	}

	public int getNumberActivations(int d) {
		return numberActivationsPerDay[d];
	}
	
	public void startRecord() {
		startTime = System.currentTimeMillis();
		startMinute = Calendar.getInstance().get(Calendar.MINUTE);
		startHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		startDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		
		numberActivationsPerDay[startDay] += 1; 	
		
	}

	/**
	 * Store the last cycle running time in the statistics table.
	 * Requires startTime to have been set correctly by the setOn() method.
	 * 
	 * NOTE: if running for many hours continuously, then data will remain empty (and show empty on webserver) until stopped.
	 * NOTE: Does not work if startDay and StopDay are different.  Very unlikely since Verner is off at night...
	 * @param stopTime
	 */
	public void recordInfoAfterStop() {
		int activeTimeSec;
		long stopTime;
		int i, j, stopMinute, stopHour;
		
		stopTime = System.currentTimeMillis();
		stopMinute = Calendar.getInstance().get(Calendar.MINUTE);
		stopHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

		activeTimeSec = (int) ((stopTime - startTime) / 1000);

		if (startDay != lastDay) {
			// reset first time this day is used
			activationTimePerDay[startDay] = 0;

			lastDay = startDay;

			i = 0;
			while (i < 24) {
				activationTimePerHour[i] = 0;
				i++;
			}
			numberActivationsPerDay[startDay] = 1; 	
		} 
		
		activationTimePerDay[startDay] += activeTimeSec;	// Will not work if span over 2 days
		
		// Cheat: if activation is short enough (less than 1 min), pretend it is the current hour...
		if (activeTimeSec < 60) {
			activationTimePerHour[stopHour] += activeTimeSec;			
		} else {	
			if (startHour != stopHour) {
				// Maths below can tolerate that start and stop are across different hours!
				
				activationTimePerHour[startHour] += (3600 - startMinute*60);
				activationTimePerHour[stopHour] += stopMinute*60;

				j = startHour;
				while (j++ < (stopHour-1))
					activationTimePerHour[j] += 3600;
			} else {
				activationTimePerHour[stopHour] += activeTimeSec;			
			}
		}
		
		startTime = System.currentTimeMillis();		// Just in case Stop() is called twice in a row and doubling stats...
	}

}
