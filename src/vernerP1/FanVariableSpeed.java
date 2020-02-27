package vernerP1;

import lejos.hardware.port.MotorPort;

/*
 * Creates and starts a a thread that will control the Fan speed.
 */

public class FanVariableSpeed  {
	int 			power = 100;	// From 0 (=off) to 100 (Full Power)
	Thread 			relayThread;
	LongPwmRelay 	theMotor;
	public VernerCtrl142 theController;
		
	public FanVariableSpeed(VernerCtrl142 aController, LongPwmRelay a_lprel) {
		theController = aController;

		theMotor = a_lprel;
		theMotor.setOff();
		setMaxSpeed();

		// super.setRelay(theMotor);
		
		relayThread = new Thread(theMotor);
		relayThread.start();
	}

	public void stopThread() {
		theMotor.stopThread();
	}
	
	public void start() {
		// theController.log("FAN ON", 2);
		theMotor.setOn();
	}

	public void setLongPWM(int on, int off, int mult) {
		theController.logWithoutDetails("Change Fan speed to " + on + "/" + off + " mult=" + mult, 4);
		theMotor.setLongPWM(on, off, mult);
	}
	
	public int getSpeed() {
		return theMotor.getSpeed();
	}
	
	public void setMaxSpeed() {
		setLongPWM(100,0,1);
	}

	public void setSlowSpeed() {
		setLongPWM(25,50,10);
	}

	public void setHalfSpeed() {
		setLongPWM(50,50,10);
	}

	public void setHighSpeed() {
		setLongPWM(75,25,10);
	}

	
	public void stop() {
		// theController.log("FAN OFF", 2);
		theMotor.setOff();
	}

	public boolean isOn() {
		return theMotor.isMoving();
	}

	public int getActivationTime(int h) {
		return theMotor.getRecorder().getActivationTime(h);
	}

	public int getNumberActivations(int d) {
		return theMotor.getRecorder().getNumberActivations(d);
	}

	public int getDailyActivationTime(int d) {
		return theMotor.getRecorder().getDailyActivationTime(d);
	}


}
