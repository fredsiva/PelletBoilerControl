package vernerP1;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

// TODO: add rolling menu:   SkipPhase, StopAll, Feed3s, 1Grating, PumpOn, PumpOff, FanOn, FanOff, IgnOn, IgnOff


public class Menu implements Runnable {
	private VernerCtrl142 theController;
	private boolean pleaseStop = false;
	private String[] lineMenu= new String[] { 
				"menu L/R",		// 0 
				"SkipPhase", 	// 1
				"StopAll", 		// 2
				"Feed10s", 		// 3
				"1Grating", 	// 4
				"WPumpOn", 		// 5
				"WPumpOff", 	// 6
				"FanOn", 		// 7
				"FanOff", 		// 8
				"IgnOn", 		// 9
				"IgnOff",		// 10
				"NextDisplay", 	// 11
				"Silent", 		// 12
				"Non-Silent", 	// 13
				"HPumpOn", 		// 14
				"HPumpOff", 	// 15
				"Quit", 		// 16
				};	
	
	private int lineMenuChoice = 0;
	
	public Menu(VernerCtrl142 aController) {
		theController = aController;
		pleaseStop = false;
	}
	
	public void executeMenuChoice(int theLineMenuChoice) {
		if (theLineMenuChoice == 1)
			theController.skipCurrentPhase();
		else if (theLineMenuChoice == 2)
			theController.forceQuit();
		else if (theLineMenuChoice == 3)
			theController.getConveyor().feedForSeconds(10);
		else if (theLineMenuChoice == 4)
			theController.getConveyor().oneGratingCycle(true);
		else if (theLineMenuChoice == 5)
			theController.getWPump().start();
		else if (theLineMenuChoice == 6)
			theController.getWPump().stop();
		else if (theLineMenuChoice == 7)
			theController.getFan().start();
		else if (theLineMenuChoice == 8)
			theController.getFan().stop();
		else if (theLineMenuChoice == 9)
			theController.getIgnitor().start();
		else if (theLineMenuChoice == 10)
			theController.getIgnitor().stop();
		else if (theLineMenuChoice == 11)
			theController.getScreen().nextDisplayMode();
		else if (theLineMenuChoice == 12)
			theController.theAlarmMonitor.goSilentMode();
		else if (theLineMenuChoice == 13)
			theController.theAlarmMonitor.goNonSilentMode();
		else if (theLineMenuChoice == 14)
			theController.getHeatingPump().start();
		else if (theLineMenuChoice == 15)
			theController.getHeatingPump().stop();
		else if (theLineMenuChoice == 16)
			theController.forceQuit();
	}

	public void pleaseStop() {
		pleaseStop = true;
	}

	public void run() {
		int buttonI = 0;
		
		while(pleaseStop == false) {
			
			LCD.clear(6);
			LCD.drawString(lineMenu[lineMenuChoice], 0, 6);
			
			buttonI =Button.getButtons();
			
			// wait Button release
			while (Button.getButtons() !=0) {
				Delay.msDelay(5);				
			}
			
			if (buttonI ==Button.ID_DOWN) {
				theController.setTargetWTemp(theController.getTargetWTemp()-1);
			} else if (buttonI==Button.ID_UP) {
				theController.setTargetWTemp(theController.getTargetWTemp()+1);
			} else if (buttonI==Button.ID_RIGHT) {
				lineMenuChoice+=1;
				if (lineMenuChoice >= lineMenu.length)
					lineMenuChoice = 0;
			} else if (buttonI==Button.ID_LEFT) {
				lineMenuChoice-=1;
				if (lineMenuChoice < 0)
					lineMenuChoice = lineMenu.length-1;
			} else if (buttonI==Button.ID_ESCAPE) {
				theController.log("Forcing Stop phase by Menu Esc", 2);;
				theController.forceQuit();
			} else if (buttonI==Button.ID_ENTER) {
				theController.log("Line Menu Choice : " + lineMenu[lineMenuChoice], 2);
				executeMenuChoice(lineMenuChoice);
			}

			Delay.msDelay(50);
		}
		
		theController.log("Menu Thread Stopping", 1);;	
	}
}
