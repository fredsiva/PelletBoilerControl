package vernerP1;

import lejos.hardware.port.*;
// import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.NXTTouchSensor;

public class ContactSensor extends NXTTouchSensor {
	float[] samples = new float[1];
	
	public ContactSensor(Port port) {
		super(port);
	}

	public int getValue() {
		getTouchMode().fetchSample(samples, 0);
		return (int) (samples[0]);
	}
}
