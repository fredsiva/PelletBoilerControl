package vernerP1;

/*
 * TBD: make it Runnable, to be able to detect too long activation in case of Bug
 */

public class Ignitor  {
	private VernerCtrl142 theController;
	private Relay theMotor;
	
	public Ignitor(VernerCtrl142 aController, Relay port) {
		theMotor = port;
		theController = aController;
	}

	public void start() {
		theController.log("Ignitor ON", 4);
		theMotor.setOn();
	}

	public void stop() {
		theController.log("Ignitor OFF", 4);
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
