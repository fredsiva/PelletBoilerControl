package vernerP1;

import java.nio.ByteBuffer;
import java.util.Calendar;

import lejos.utility.Delay;

public class WaterPump {
	ArduinoI2CLink theArduino;
	private VernerCtrl142 theController;
	private Recorder theRecorder;
	
	private int activationTimePerDay[] = new int[356];
	private int activationTimePerHour[] = new int[24];
	private int numberActivationsPerDay[] = new int[356];
	private int lastHour = -1, 
			stopHour, 
			lastDay = -1, 
			day;
	private long startTime = 0;
	private boolean 		isCircOn;
	private byte[] 		bufReadResponseLH = new byte[2];
	private double 		result = 0;
	private ByteBuffer 	wrapped;
	private char CmdOn, CmdOff;
	
	public WaterPump(VernerCtrl142 aController, ArduinoI2CLink arduino, char aCmdOn, char aCmdOff) {
		theArduino = arduino;
		theController = aController;
		theRecorder = new Recorder();
		CmdOn = aCmdOn;
		CmdOff = aCmdOff;
	}

	public Recorder getRecorder() {
		return theRecorder;
	}
	
	public boolean isOn() {
		return isCircOn;
	}

	public synchronized double start() {
		try {
			theArduino.getData(CmdOn, bufReadResponseLH, bufReadResponseLH.length);
			isCircOn = true;
		} catch (Exception e) {
			theController.logWithoutDetails("Error turning circ On Cmd=[" + CmdOn + " ] on Arduino" + e, 0);
			return 0;
		}

		wrapped = ByteBuffer.wrap(bufReadResponseLH);
		result = wrapped.getShort();
		theRecorder.startRecord();

		Delay.msDelay(10);	

		return ((double) result);
	}

	public synchronized double stop() {
		if (isCircOn) {
			theRecorder.recordInfoAfterStop();
		}

		try {
			theArduino.getData(CmdOff, bufReadResponseLH, bufReadResponseLH.length);
			isCircOn = false;
		} catch (Exception e) {
			theController.logWithoutDetails("Error turning circ Off Cmd=[" + CmdOff + " ] on Arduino" + e, 0);
			return 0;
		}

		wrapped = ByteBuffer.wrap(bufReadResponseLH);
		result = wrapped.getShort();
		Delay.msDelay(10);	

		return ((double) result);
	}

}
