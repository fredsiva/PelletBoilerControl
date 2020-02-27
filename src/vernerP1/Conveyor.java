package vernerP1;

import lejos.hardware.Button;
import lejos.utility.Delay;

/*
 * Synchronization (multi-thread) Requirements:
 * 		Feed and Grating can never be called at the same time, and should be well separated to ensure life of the relay
 * 		In case of emergency fire, feed should be called rapidly without having to wait too long
 * 		If necessary, one grating can be interrupted by a feed, but the grating must then still complete its cycle just after. 
 */
public class Conveyor implements Runnable {
	private VernerCtrl142 theController;
	private Relay relayConveyor, relayGrating;
	private ContactSensor theContact;
	int gratingTimeoutSec;
	long lastPerfectGrating = 0;

	private int WAIT_MILLISEC_MOTOR_STOPS = 1000;
	
	private boolean	pleaseStop = false;
	int feedTimeS, waitTimeS;
		
	public Conveyor(VernerCtrl142 aController, Relay aRelayConveyor, Relay aRelayGrating, ContactSensor aContact, int gratingSec) {
		theController = aController;
		relayConveyor = aRelayConveyor;
		relayGrating = aRelayGrating;
		theContact = aContact;
	
		feedTimeS = 0;
		waitTimeS = 1;
		
		gratingTimeoutSec = gratingSec;
	}

	public void setRunningTimeSec(int time) {
		feedTimeS = time;
	}

	public void pleaseStop() {
		pleaseStop = true;
	}

	/*
	 * Allows to resume (by calling restart()) the thread after a stop().
	 */
	public void pleasePrepareForRestart() {
		pleaseStop = false;
	}

	public void setWaitingTimeSec(int time) {
		waitTimeS = time;
	}

	/*
	 * Change timing of the feeding process.
	 * Since the previous timing can potentially still be in long waiting mode, the waiting will be set to zero to shorten it.
	 * Drawback: during next change of timing, because of this waitS reset, there will be one more feed!
	 */
	public synchronized void setTimes(int runS, int waitS) {
		setRunningTimeSec(runS);

		setWaitingTimeSec(0);	// To force the run() loop to exit
		Delay.msDelay(500);

		setRunningTimeSec(runS);
		setWaitingTimeSec(waitS);
	}

	/*
	 * Changes the timing of the feed/wait, but if the conveyor is in a long waiting cycle, it will just wait that cycle is completed. 
	 */
	public void setTimesAfterWaiting(int runS, int waitS) {
		setRunningTimeSec(runS);
		setWaitingTimeSec(waitS);
	}
	
	public String getTimeS() {
		return ("" + feedTimeS + "," + waitTimeS);
		
	}
	
	public int getFeedHourlyActivationTime(int h) {
		return relayConveyor.getRecorder().getActivationTime(h);
	}

	public int getFeedDailyActivationTime(int d) {
		return relayConveyor.getRecorder().getDailyActivationTime(d);
	}

	public int getFeedNumberActivations(int d) {
		return relayConveyor.getRecorder().getNumberActivations(d);
	}

	public int getGratingHourlyActivationTime(int h) {
		return relayGrating.getRecorder().getActivationTime(h);
	}

	public int getGratingDailyActivationTime(int d) {
		return relayGrating.getRecorder().getDailyActivationTime(d);
	}

	public int getGratingNumberActivations(int d) {
		return relayGrating.getRecorder().getNumberActivations(d);
	}
	
	/*	TODO: run feed() of equivalent time after grating() so that pellets are close to the fireplace.  But use boolean to avoid this in afterburn.ox
	 * 
	 * 	Nettoyage du foyer.
	 *  Démarrer le moteur Conveyor à l'envers (pilote le second relai pour moteur triphasé), 
	 *  et le fait tourner jusqu'à ce que le contacteur de fin de course se re-ouvre (Logique inverse du contact)
	 *  
	 *  Returns false in case the timeout has been reached.
	 *  
	 *  In rest state, contact is open (value == 0), during the travel, it then closes (value = 1)
	 *  
	 *  
	 */
	public synchronized boolean oneGratingCycle(boolean reFeedAfter) {
		long startTime;
		boolean contactClosed = false, 
				contactOpenInitially;
		
		// Because of Synchronization, we can assume here that we are not in a feeding cycle.
		theController.log("One Grating Cycle with refeed =" + reFeedAfter, 3);

		theController.getScreen().displayStatus("Grating ....", false);

		if (theContact.getValue() != 0) {
			theController.log(
					"Verner Warning - Grating - Contact is not correctly open before starting grating phase (Open = rest position = value 0)," + 
					"The last perfect grating was " + ((System.currentTimeMillis() - lastPerfectGrating)/60000.0) + " mins ago", 2);
			contactOpenInitially = false;
		} else {
			theController.log("Contact is correctly open before starting Grating cycle.", 4);
			contactOpenInitially = true;
		}
	
		startTime = System.currentTimeMillis();
		startGrating();

		startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < (gratingTimeoutSec*1000) &&
				contactClosed == false){
			if 	(theContact.getValue() == 1) {
				if (contactOpenInitially == true) { 
					theController.log("Contact closes after " + (System.currentTimeMillis() - startTime) + 
							"msecs, means Relay2, Conveyor motor and contactor are all 3 perfectly working", 3);
					lastPerfectGrating = startTime;
				}
				contactClosed = true;
			} else {
				// Waiting that contact closes during the travel
				Delay.msDelay(100);
			}		
		}

		if (contactClosed == false) {
			// Not normal, the contact should close soon after grating starts
			theController.log("Contact did not correctly close when soon after starting Grating cycle.  Will stop grating", 1);

			stopGrating();

			VernerCtrl142.theAlarmMonitor.sendEmailAvoidingFlood(
					"Verner WARNING - Grating", "Contact did not correctly close soon after starting grating, will stop grating now. " +  
					"(Open = rest position = value 0), timeout is " +  gratingTimeoutSec + "secs\n"+
					"The last perfect grating was " + ((System.currentTimeMillis() - lastPerfectGrating)/60000.0) + " mins ago" +
					theController.statusHelper.getLatestEventsString());

			// Delay.msDelay(1000);
			if (reFeedAfter) {
				feedForSeconds((int) ((System.currentTimeMillis() - startTime)/1000));
			}
			return false;
		} else {
			// Contact has correctly closed at the beginning of the travel, so let's monitor the opening now (=end of travel)
			// startTime is un-changed, so the Timeout relates to the full grating cycle
			while (System.currentTimeMillis() - startTime < (gratingTimeoutSec*1000)) {
				if (theContact.getValue() == 0) {
					stopGrating();

					theController.getScreen().displayStatus("Grating fully OK", false);

					// For info, let's check if the contact is still open (may not be the case of some inertia in motor)
					
					if (theContact.getValue() != 0) {
						theController.log("Contact correctly openned, but because of motor inertia, it is not openned anymore." +
											"Next Grating will produce a warning" + 						
											"The last perfect grating was " + ((System.currentTimeMillis() - lastPerfectGrating)/60000.0) + " mins ago", 3);

					}
					
					Delay.msDelay(1000);

					if (reFeedAfter) {
						feedForSeconds((int) ((System.currentTimeMillis() - startTime)/1000));
					}
					return true;
				}
			}
		}
		
		// Timeout, should not happen...
		stopGrating();
		
		Delay.msDelay(1000);
		VernerCtrl142.theAlarmMonitor.sendEmailAvoidingFlood(
						"Verner ERROR - Grating Timed-out", 
						"The grating had to be stopped before contact trigger (Timeout =" + gratingTimeoutSec + "sec)\n"+
  						"The last perfect grating was " + ((System.currentTimeMillis() - lastPerfectGrating)/60000.0) + " mins ago" +
						theController.statusHelper.getLatestEventsString());

		theController.getScreen().displayStatus("Grating Timeout" , false);

		if (reFeedAfter) {
			theController.log("Refeed =" + reFeedAfter, 2);

			feedForSeconds((int) ((System.currentTimeMillis() - startTime)/1000));
		}

		return false;
	}

	/*
	 * First stop completely motor, then commute to Feed, only then start motor.
	 * 
	 * This is a low-level method, use oneGratingCycle() to control the duration of the grating with the limit switch.
	 * Returns the total delay time incurred before grating acually starts.
	 */
	private synchronized int startGrating() {
		// First ensure that motor is totally off
		relayConveyor.resetStartTime();		// Because setOff() will be called with a previous setOn()
		relayConveyor.setOff();
		
		Delay.msDelay(WAIT_MILLISEC_MOTOR_STOPS);

		relayGrating.setOn();
		Delay.msDelay(WAIT_MILLISEC_MOTOR_STOPS / 2);

		relayConveyor.setOn();

		return (WAIT_MILLISEC_MOTOR_STOPS + WAIT_MILLISEC_MOTOR_STOPS/2);
	}
	
	/*
	 * Stop is immediate, but will followed by some delays to safely close the 2nd relay.
	 * 
	 * IMPORTANT NOTE : HOW TO AVOID KILLING RELAYS
	 * 	- The 2nd relay (relayGrating) can absolutely NOT be setOff() if the conveyor is still running (and delivering current).
	 */
	private synchronized void stopGrating() {
		relayConveyor.setOff();	// always start by shutting down the conveyor motor (and current feed to 2nd relay)
		Delay.msDelay(WAIT_MILLISEC_MOTOR_STOPS);		// Always wait before commute

		relayGrating.setOff();	// back to feed mode
		Delay.msDelay(100);	// In case the motor is activated right after this method
	}

	/**
	 * returns the waiting time within feed(), during which the conveyer screw is NOT running.
	 */
	private synchronized int startFeed() {
		// First ensure that motor is totally off (Can be rather slow if Grating was in progress, because of Inertia of big wheel)
		relayConveyor.resetStartTime();		// Because setOff() will be called with a previous setOn()
		relayConveyor.setOff();
		Delay.msDelay(WAIT_MILLISEC_MOTOR_STOPS);

		relayGrating.resetStartTime();		// Because setOff() will be called with a previous setOn()
		relayGrating.setOff();
		Delay.msDelay(100);

		relayConveyor.setOn();
		Delay.msDelay(100);	// In case the motor is activated right after this method
		
		return (WAIT_MILLISEC_MOTOR_STOPS + 100);
	}
	
	private synchronized void stopFeed() {
		relayConveyor.setOff();		// Do not care about Grating Relay since second in serie
		Delay.msDelay(500);	// In case the motor is activated right after this method
	}

	/*
	 * Test methods wrapper.
	 */
	public void testGrating() { 		startGrating(); }
	public void testStopGrating() { 	stopGrating(); }
	public void testFeed() { 			startFeed(); }
	public void testStopFeed() { 		stopFeed(); }
	
	/*
	 * Cannot be interrupted by a change of feeding time, but only by pleaseStop().
	 * 
	 * Aware of the delays within feed, and compensate by feeding slightly longer.
	 */
	public synchronized void feedForSeconds(int durationSec) {
		long startTime = System.currentTimeMillis();
		int waitingTimeDuringFeedMillisecs;
		
		theController.log("Feed for " + durationSec + " seconds", 4);
		waitingTimeDuringFeedMillisecs = startFeed();

		theController.log("Feed will be increased by " + waitingTimeDuringFeedMillisecs + " millisecs as compensation for waiting time", 4);
		
		// An active loop is better to allow interruption by pleaseStop();
		while ( ((System.currentTimeMillis() - startTime) < (durationSec*1000 + waitingTimeDuringFeedMillisecs) ) && 
				pleaseStop == false) {
			Delay.msDelay(100);
		}
		
		Button.LEDPattern(0);
		theController.log("Feed completed (pleaseStop =" + pleaseStop + ")" , 4);

		stopFeed();	
	}

	/*
	 * Runs forever
	 */
	public void run() {
		long startTime;

		theController.log("Conveyor run() method started", 2);

		while(pleaseStop == false) {
			// runPaused = false;
			
			if (feedTimeS > 0) {
				feedForSeconds(feedTimeS);
			}
			
			startTime = System.currentTimeMillis();
			
			// An active loop is better since waitTimeS can be changed any time
			while ( ((System.currentTimeMillis() - startTime) < waitTimeS * 1000 ) && 
					pleaseStop == false) {
				Button.LEDPattern(1);
				Delay.msDelay(50);
				Button.LEDPattern(0);
				Delay.msDelay(300);
			}
			
			Button.LEDPattern(0);
		}
		
		theController.log("Conveyor run() Thread Stopping", 1);

		stopFeed();
	}
}
