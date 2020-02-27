package vernerP1;

import java.util.Calendar;

import lejos.hardware.Battery;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

/**
 * Line 0: Temps
 * Line 1: Feed Times, WP, Ignition indicator, Grating Contactor
 * Line 2: State, Voltage, Current
 * Line 3: Target temp during Ignition
 * Line 4: 
 * Line 5: Status
 * Line 6: menu left/right
 * 
 * @author fsiva
 *
 */
public class ScreenAndStats implements Runnable {
	
	private VernerCtrl142 theController;
    private boolean pleaseStop = false;
    private int lastDay, newDay, lastHour, newHour;
    private int screenModeI = 0;
	GraphicsLCD g;
	private int SW;
	private int SH;
	private int lineH;
	private String lastMessageDisplayed = new String("");
	
	public ScreenAndStats(VernerCtrl142 aController) {
		theController = aController;

		lastHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		lastDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

		g = BrickFinder.getDefault().getGraphicsLCD();
		SW = g.getWidth();
		SH = g.getHeight();
		lineH = Font.getLargeFont().height + 5;
		
		LCD.clear();
	}
	
	/**
	 * 		ScreenMode: 0 is verbose, 1 is minimal
	 */
	public void nextDisplayMode() {
	 
		screenModeI++;
		
		if (screenModeI >1) {
			screenModeI = 0;
		}

		LCD.clear();
		refreshLCD();	
	}
	
	public void run() {
		while (pleaseStop == false) {
			refreshLCD();
			Delay.msDelay(1000 * 5);
		}
		theController.log("Screen Refresher Thread Stopping", 1);
	}

	public void pleaseStop() {
		pleaseStop = true;
	}
	
	private void logStatsIfNecessary() {
		float tempF;
		
		newHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		newDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

		if (newHour != lastHour) {
			tempF = theController.getWeatherApp().getLocalTempFromOpenWeatherApp();

			theController.statusHelper.dumpHourlyStats();
			lastHour = newHour;
		}

		if (newDay != lastDay) {
			lastDay = newDay;
		}
	}

	private void refreshLCD_verbose() {
		LCD.clear(0);
		LCD.clear(1);
		LCD.clear(2);
		LCD.clear(4);

		LCD.drawString(	"T" + theController.getTargetWTemp() +
						" W" + theController.getStoredWTemp() + 
						" G" + theController.getStoredExhTemp() + 
						" V" + + theController.getScrewTemp() + " ", 0, 0);

		LCD.drawString("F,W=" + theController.getConveyor().getTimeS()+ " " + theController.getFan().getSpeed()+"% ", 0,1);

		if (theController.getWPump().isOn())
			LCD.drawString("Wp", 13, 1);

		if (theController.isIgnitorOn())
			LCD.drawString("@", 15, 1);

		LCD.drawString("C" + theController.getContactorValue(),  16, 1);
		
		LCD.drawString("S" + theController.getState() + " T" + theController.getThermostatStatus() + " " + 
						String.format("%.2g", Battery.getVoltage()) + "V " + 
						String.format("%3g", Battery.getBatteryCurrent()*1000).substring(0,3) + "mA", 0, 2);

		LCD.drawString(	"E" + theController.getStoredChemineeTemp() +
				" B" + theController.getStoredBoilerTemp() + 
				" R" + theController.getStoredRoomTemp() + " ", 0, 4);
		
	}

	private void refreshLCD_minimal() {
		
		// Clear first Line
		g.setColor(GraphicsLCD.WHITE);
		g.fillRect(0, 0, SW, lineH);	

		g.setColor(GraphicsLCD.BLACK);
		g.setFont(Font.getLargeFont());
		
		g.drawString("" + theController.getTargetWTemp() , 3, 4, GraphicsLCD.TOP|GraphicsLCD.LEFT);
			 g.drawRect(2, 2, Font.getLargeFont().width * 3, Font.getLargeFont().height);
		
		g.drawString("" + theController.getStoredWTemp() , SW/2, 4, GraphicsLCD.TOP|GraphicsLCD.LEFT);

		
		if (theController.getWPump().isOn())
			LCD.drawString("W", 14, 0);

		if (theController.getHeatingPump().isOn())
			LCD.drawString("H", 15, 0);

		if (theController.isIgnitorOn())
			LCD.drawString("@", 16, 0);
		
		LCD.drawString("T" + theController.getThermostatStatus(),  14, 1);
		LCD.drawString("G" + theController.getStoredExhTemp() + " ", 14, 2);
		LCD.drawString("V" + theController.getScrewTemp() + " ", 14, 3);
		LCD.drawString("S" + theController.getState(), 14, 4); 

	}
	
	public void refreshLCD() {
		if (screenModeI == 1) 
			refreshLCD_minimal();
		else
			refreshLCD_verbose();
		
		logStatsIfNecessary();
	}

	public void showTargetIgnitionWT(int temp) {
		if (screenModeI == 0)
			LCD.drawString("<" + temp + " ", 12, 3);
	}

	public void clearTargetIgnitionWT() {
		if (screenModeI == 0)
			LCD.drawString("      ", 12, 3);
	}

	public void displayStatus(String str) {
		displayStatus(str, false);
	}

	// TODO: ensure system.err does not appear on screen, and use LCD.drawString instead. (Not easy...)
	public void displayFinalErr(String str) {
		System.err.println("---");
		System.err.println(str);
		System.err.println("Press Key");

		Button.waitForAnyPress();  
	}
	
	public void displayStatus(String str, boolean waitKey) {
		// Clear line first

		if (screenModeI == 1) {
			g.setColor(GraphicsLCD.WHITE);
			g.fillRect(0, lineH, SW-30, lineH*2);	
			g.setColor(GraphicsLCD.BLACK);

			g.drawString("" + theController.getStateString() + " " , 3, lineH, GraphicsLCD.TOP|GraphicsLCD.LEFT);
			
		} else {
			LCD.clear(5);
			LCD.drawString(str, 0, 5);
		}  

		if (lastMessageDisplayed.equalsIgnoreCase(str) == false) {
			// Only log the Displayed message the first occurence.
			theController.log("Display:" + str + "  ", 3);
			lastMessageDisplayed = str;
		}

		if (waitKey == true){
			LCD.clear(6);

			LCD.drawString("Press Key", 0, 6);
			Button.waitForAnyPress();
			LCD.clear(6);
		}


	}

}
