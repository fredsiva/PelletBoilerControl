package vernerP1;

import java.nio.ByteBuffer;
import lejos.utility.Delay;

public class ArduinoContactSensor  {
	private ArduinoI2CLink theArduino;
	private VernerCtrl142 theController;

	private byte[] 		bufReadResponseLH = new byte[2];
	private ByteBuffer 	wrapped;
	private char 		theCommandChar = 'Z';
	private int			isThermostatOn = 0;
	
	/*
	 * aCommandChar is the letter expected by Arduino as command.
	 */
	public ArduinoContactSensor (VernerCtrl142 aController, ArduinoI2CLink arduino, char aCommandChar) {
		theArduino = arduino;
		theController = aController;
		theCommandChar = aCommandChar;
	}
	
	/*
	 * Returns 1 if Thermostat is ON
	 * Returns 0 if Thermostat is OFF
	 * Returns -1 in case of error
	 */
	public int getValue() {
		try {
			bufReadResponseLH[0] = 0;
			bufReadResponseLH[1] = 0;

			theArduino.getData(theCommandChar, bufReadResponseLH, bufReadResponseLH.length);	// Already includes a retry mechanism

			wrapped = ByteBuffer.wrap(bufReadResponseLH);
			isThermostatOn = wrapped.getShort();
			
			theController.logWithoutDetails("Got value from Arduino cmd [" + theCommandChar + "], value = " + isThermostatOn, 4);

			// Valid temp, return it
			Delay.msDelay(1000);		// Wait a bit in case another getValue is attempted right after.
			return isThermostatOn;
				
		} catch (Exception e) {
			theController.logWithoutDetails("Error getting Arduino Thermostat value Cmd [" + theCommandChar + 
					"], will reset Arduino and then retry in 5 Sec: " + e, 0);

			theArduino.resetArduinoAndWait5Sec();
			return -1;
		}
		
	}
}
