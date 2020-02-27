package vernerP1;

import java.nio.ByteBuffer;

import lejos.utility.Delay;

public class ArduinoTempSensor  {
	private ArduinoI2CLink theArduino;
	private VernerCtrl142 theController;
	private byte[] 		bufReadResponseLH = new byte[2];
	private ByteBuffer 	wrapped;
	private int			waterTempI;
	private TempCollector	theRecorder;
	private char 			theCommandChar = 'Z';
	private float a,b;
	private int lastTemp;
	
	/*
	 * aCommandChar is the letter expected by Arduino as command.
	 */
	public ArduinoTempSensor (VernerCtrl142 aController, ArduinoI2CLink arduino, String aNameS, char aCommandChar) {
		theArduino = arduino;
		theController = aController;
		theRecorder = new TempCollector(aController, aNameS);
		theCommandChar = aCommandChar;
		a = 1;
		b = 0;
	}

	public void calibrateLine(float temp1, float value1, float temp2, float value2) {
		// temp = a.value + b

		a =  (temp2 - temp1) / (value2 - value1);	// pente
		b = temp1 - a*value1;

		theController.log("Calibrated sensor (" + this + ") with a=" + a + " and b=" + b, 2);
	}

	
	public TempCollector getRecorder() {
		return theRecorder;
	}

	public int getCalibratedTemp() {
		try {
			lastTemp =(int) (getRawValue() * a +b);
		} catch (Exception e) {
			theController.log("Exception getting value from Arduino Temp Sensor : " + e, 0);
			return 0;
		}

		theRecorder.recordInfo(lastTemp);

		return lastTemp;
	}

	/*
	 * Synch method since called from ScreenRefresher() but also from Run()
	 * 
	 * the method getValue() called has a simple retry mechanism
	 * this method here has a more radical approach: it will reset the Arduino in case it does provide a valid temp
	 */
	private synchronized double getRawValue() {
		int retryLeft = 3;
		
		while (retryLeft-- > 0) {
			try {
				bufReadResponseLH[0] = 0;
				bufReadResponseLH[1] = 0;

				theArduino.getData(theCommandChar, bufReadResponseLH, bufReadResponseLH.length);	// Already includes a retry mechanism

				wrapped = ByteBuffer.wrap(bufReadResponseLH);
				waterTempI = wrapped.getShort();
				
				theRecorder.recordInfo(waterTempI / 100);

				theController.logWithoutDetails(
						"Got value back from Arduino command [" + theCommandChar + "], value = " + waterTempI, 
						4);

				
				if (waterTempI < 10*100 || waterTempI > 1000*100) {
					// Impossible temp (Lower than 10 or Higher than 1000), Reset Arduino
					theController.logWithoutDetails("Impossible Temp [" + theCommandChar + "] ,"
							+ " must be an error... (Will retry in 5 sec). Temp in cents was : " + waterTempI, 0);
					
					theArduino.resetArduinoAndWait5Sec();
				} else {
					// Valid temp, return it
					Delay.msDelay(1000);		// Wait a bit in case another getValue is attempted right after.
					return ((double) waterTempI) / 100.0;
					
				}				
			} catch (Exception e) {
				theController.logWithoutDetails("Error getting Arduino Temp Cmd [" + theCommandChar + "], will reset Arduino and then retry in 5 Sec: " + e, 0);

				theArduino.resetArduinoAndWait5Sec();
			}
		
		}

		theController.logWithoutDetails("Error reading temp [" + theCommandChar + "] after several retries... (Will be set to zero). Temp was : " + waterTempI, 0);

		waterTempI = 0;					
		Delay.msDelay(1000);		// Wait a bit in case another getValue is attempted right after.
		return ((double) waterTempI) / 100.0;
	}
}
