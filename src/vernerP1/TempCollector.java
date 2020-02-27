package vernerP1;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * 
 * Collects temperatures during 24h
 * 
 * recordInfo() can be called at any time with temperature, and will be compared to previous calls during 
 * the same hour to detect Min and Max.
 * 
 * The average is not a true integral, but Min+Max/2
 *
 */
public class TempCollector {
	private int maxTempDuringHour[] = new int[24];
	private int minTempDuringHour[] = new int[24];
	private int avgTempDuringHour[] = new int[24];

	private List<tempHistoricalRecord> tempHistory = new LinkedList<tempHistoricalRecord>();
	
	private VernerCtrl142 theController;

	private int lastHour, newHour, 
				lastTemp = 0;
	long lastCallTime = -1, 
		 deltaSec;

	private String nameS;
	private tempHistoricalRecord tRec;
	
	public TempCollector(VernerCtrl142 aController, String aNameS) {
		theController = aController;
		lastHour = -1;
		nameS = aNameS;
	}

	private List<tempHistoricalRecord> getTempHistory() {
		return tempHistory;
	}
	
	/*
	 * Note: should not call log() from here for recursivity issues: 
	 * 		since logging calls temp.getValue() which itself calls recordInfo
	 */
	public void recordInfo(int value) {
		newHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		deltaSec = (System.currentTimeMillis() - lastCallTime)/1000;
		
		/** Not used and not tested, and remove(0) returned once a null pointer...

		if (deltaSec != 0) { 
			tRec = new tempHistoricalRecord(lastCallTime, value, "/", (float) (value-lastTemp) / ((float) deltaSec / 60f));
		} else {
			tRec = new tempHistoricalRecord(lastCallTime, value, "No Speed", 0);
		}

		tempHistory.add(tRec);
		lastTemp = value;
		lastCallTime = System.currentTimeMillis();

		// Remove oldest record if too big
		while (tempHistory.size() > 10) {
			try {
				tempHistory.remove(0);
			} catch (Exception e) {
				theController.logWithoutDetails("Error removing historical record from tempHistory" + e, 1);
			}
		}
		*/
		
		if (newHour != lastHour) {
			maxTempDuringHour[newHour] = value;
			minTempDuringHour[newHour] = value;
			
			if (lastHour != -1)
				avgTempDuringHour[lastHour] = (maxTempDuringHour[lastHour] + minTempDuringHour[lastHour]) /2;

			lastHour = newHour;

			return;
		} else {
			if (value > maxTempDuringHour[newHour])
				maxTempDuringHour[newHour] = value;

			if (value < minTempDuringHour[newHour])
				minTempDuringHour[newHour] = value;			
		}

	}

	public int getMinTemp(int h) {
		return minTempDuringHour[h];
	}

	public int getMaxTemp(int h) {
		return maxTempDuringHour[h];
	}
}
