package vernerP1;

import java.nio.ByteBuffer;
import java.util.Calendar;

import lejos.hardware.Button;
import lejos.hardware.port.I2CException;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.I2CSensor;
import lejos.hardware.sensor.SensorConstants;
import lejos.utility.Delay;

/*
 * Do not use Log() from the methods here, since logging will fetch the temperature to display them...
 */
public class ArduinoI2CLink {
   private int 				I2CSlaveAddress = 8;
   private I2CSensor 		arduino;
   private VernerCtrl142 	theController;
   private int				retryCount = 5;
   
   /*
    * Mimic the getData method of I2C class, but the retry here nicely includes some Delay.
    */
   public synchronized void getData(int register, byte [] buf, int len) {
	   I2CException error = null;
	   int i;

	   // Delay.msDelay(30);	// Not necessary if Arduino is not slowed down by Serial traces...	

	   for(i = 0; i < retryCount; i++) {
		   try {
			   // theController.logWithoutDetails("Send query to Arduino [" + register + "]", 2);

			   arduino.getData(register, buf, len);

			   // theController.logWithoutDetails("Send Command [" + register + "] to Arduino , Get answer[" + buf + "] of length=" + len, 2);

			   return;
		   } catch (I2CException e) {
			   error = e;
			   theController.logWithoutDetails("Error getting Arduino Temp, will retry (" + i + "/" + retryCount + ")" + e, 0);
		   }
	
		   Delay.msDelay(30*(i+1));	
	   }
	   throw error;
   }
	
   public synchronized void resetArduinoAndWait5Sec() {
		byte[] 		bufReadResponseLH = new byte[2];

		theController.logWithoutDetails("Will fully reset the Arduino by sending an X ===", 0);
		theController.logWithoutDetails("ResetArduino: FAKE IT FOR NOW AND JUST WAIT 5 Sec & RETURN", 0);

		/*
		try {
			// Use direct I2CSensor version to avoid retrying
			arduino.getData('X', bufReadResponseLH, bufReadResponseLH.length);
		} catch (I2CException e) {
			   theController.logWithoutDetails("Normal behaviour, reset Arduino will never get an I2C response! " + e, 0);
		}
		
		TODO: Recreate an I2C connection? with new I2CSensor()?
		*/
		
		// Wait 5 Secs
		Delay.msDelay(1000*5);	

		theController.logWithoutDetails("After Full Reset - I2C Init Arduino Highspeed=[" + arduino.getVersion() +
				   "] Vdr= [" + arduino.getVendorID() + 
				   "] Prd=[" + arduino.getProductID() + 
				   "] Adr=[" + arduino.getAddress() + "]" +
				   "] Retry=[" + arduino.getRetryCount() + "]"
				   , 2);	   
		
   }
   
   public void initArduinoI2CLink(VernerCtrl142 aController, Port port) {
	   theController = aController;

	   arduino = new I2CSensor(port, I2CSlaveAddress, SensorConstants.TYPE_HIGHSPEED);

	   arduino.setRetryCount(10);
	   
	   theController.log("I2C Init Arduino Highspeed=[" + arduino.getVersion() +
			   "] Vdr= [" + arduino.getVendorID() + 
			   "] Prd=[" + arduino.getProductID() + 
			   "] Adr=[" + arduino.getAddress() + "]" +
			   "] Retry=[" + arduino.getRetryCount() + "]"
			   , 2);	   
   }
   
   public void closeLink() {
	   arduino.close();
   }
}