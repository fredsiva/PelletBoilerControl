package vernerP1;

import java.util.Calendar;

import lejos.hardware.port.*;
import lejos.hardware.sensor.NXTLightSensor;

public class AmbientTempSensor {
	private NXTLightSensor sensor;
	private VernerCtrl142 theController;
	private float[] samples = new float[1];
	private float a,b;
	private int lastTemp;
	private TempCollector	theRecorder;

	public AmbientTempSensor(VernerCtrl142 aController, Port port, String aNameS) {
		sensor = new NXTLightSensor(port);
		a = 1;
		b = 0;
		theController = aController;
		theRecorder = new TempCollector(aController, aNameS);
		
	}

	public TempCollector getRecorder() {
		return theRecorder;
	}
	
	public void calibrateLine(float temp1, float value1, float temp2, float value2) {
		// temp = a.value + b

		a =  (temp2 - temp1) / (value2 - value1);	// pente
		b = temp1 - a*value1;

		theController.log("Calibrated sensor (" + this + ") with a=" + a + " and b=" + b, 2);
	}

	public int getValue() {
		try {
			lastTemp =(int) (getRawValue() * a +b);
		} catch (Exception e) {
			theController.log("Exception getting value from Ambient Temp Sensor : " + e, 0);
			return 0;
		}

		theRecorder.recordInfo(lastTemp);

		return lastTemp;
	}

	public float getRawValue() {
		try {
			sensor.getAmbientMode().fetchSample(samples, 0);
		} catch (Exception e) {
			theController.log("Exception getting value from Ambient Temp Sensor : " + e, 0);
			return 0;
		}
		return (samples[0]);
	}

}
