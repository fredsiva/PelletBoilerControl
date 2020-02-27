package vernerP1;

/*
 * TBD: make it Runnable, to be able to detect too long activation in case of Bug
 */

public class Fan {
	protected Relay theMotor;
	public VernerCtrl142 theController;
	boolean isOn = false;
	
	public Fan(VernerCtrl142 aController, Relay rel) {
		theMotor = rel;
		theController = aController;
		isOn = theMotor.isMoving();	// Not guaranteed, since PWM alternates between ON and OFF very quickly!
	}

	public void setRelay(Relay rel) {
		theMotor = rel;
	}
	
	public void start() {
		// theController.log("FAN ON", 2);
		theMotor.setOn();
		isOn = true;
	}

	public void stop() {
		// theController.log("FAN OFF", 2);
		theMotor.setOff();
		isOn = false;
	}

	public boolean isOn() {
		return isOn; 	// theMotor.isMoving(); // Trick since PWM alternates quickly between on Moving and not moving...
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
