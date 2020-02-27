package vernerP1;

public class tempHistoricalRecord {
	public long time;
	public int temp;
	public String descrS;
	float speedInDegPerMinute;
	
	public tempHistoricalRecord(long aTime, int aTemp, String aDescrS, float aSpeedInDegPerMinute) {
		time = aTime;
		temp = aTemp;
		descrS = aDescrS;
		speedInDegPerMinute = aSpeedInDegPerMinute;
	}
}
