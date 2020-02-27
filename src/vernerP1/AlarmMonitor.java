package vernerP1;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;

import biz.source_code.base64Coder.Base64Coder;
import lejos.hardware.Sound;
import lejos.utility.Delay;

/*
 * Monitore la température de la vis, et Feed qqs secondes si nécessaire
 * Monitore la température de l'eau, et enclenche le circulateur si dépasse 10 deg au dessus de la consigne
 * 
 *  Envoie des alarmes email si nécessaire
 */
public class AlarmMonitor implements Runnable {
	VernerCtrl142 theController;
	long K_FEED_TEMP_ALARM_VIS_MIN = 5;
	long K_MIN_TIME_BETWEEN_EMAILS_MIN = 5;
	int K_HYSTERESIS = 2;
	int port = 2525;	// 25 
    private boolean pleaseStop = false;
	long lastEmailAlarm = 0;
	String responseline;
	Socket socket;
	boolean silentMode = false;
	
	private boolean alarmingScrewT = false,
					alarmingWaterT = false,
					alarmingExhT = false,
					warningScrewT = false;
	
	
	// String username = Base64Coder.encodeString("frederic.siva@gmail.com");
	String username = Base64Coder.encodeString("ev3.verner@siva.com");
	String password = Base64Coder.encodeString("archiduc.minerva");
	
	int tempoWaitS,
		maxTempEau,
		maxTempGaz,
		maxScrewTemp,
		warningScrewTemp,
		deltaTempTriggerECS,
		waterPumpTriggerGazTemp,
		hystereris;
	
	public AlarmMonitor(VernerCtrl142 aController, int monitoringTempoSec, int tempEau, int tempGaz, int tempVis) {
		theController = aController;
		tempoWaitS = monitoringTempoSec;
		maxTempEau = tempEau; 
		maxTempGaz = tempGaz;
		maxScrewTemp =tempVis;
		warningScrewTemp = maxScrewTemp - 5;
		hystereris = 2;
		
		deltaTempTriggerECS = 11;		// if lower than param_delta_temp_target_max, then Verner will struggle to reach target temp...
		waterPumpTriggerGazTemp = 200;		
		
		theController.log("Alarm object created WaterMax=" + maxTempEau +
						" deltaTempTriggerECS =" + deltaTempTriggerECS +
						" GazMax=" + maxTempGaz + 
						" VisMax=" + maxScrewTemp +
						" Gaz Trigger WP=" + waterPumpTriggerGazTemp +
						" Tempo = " + tempoWaitS + "sec", 
						2);	
	}

	/**
	 * 
	 * @return true if at least one temperature is abnormal.  False if ALL temps are normal.
	 */
	public boolean newAlarmingSituation() {
		if (alarmingScrewT == false && alarmingWaterT==false && alarmingExhT==false && warningScrewT==false) {
			return false;
		} else return true;
	}
	
	public void beep(){
		Sound.playTone(1760,  1000, 100);
	}

	public void beepLoud(){
		Sound.playTone(2500,  3000, 100);
	}

	public void pleaseStop() {
		theController.log("Stop Alarm Controller",1); 
		pleaseStop = true;
	}
	
	public void goSilentMode() {
		theController.log("Put Alarm Controller in Silent Mode",1); 
		silentMode = true;	
	}

	public void goNonSilentMode() {
		theController.log("Put Alarm Controller in Non-Silent Mode",1); 
		silentMode = false;	
	}

	// TODO: use DNS to resolve SMTP2GO adress
	
	/*
	 * Send email to hardcoded address.
	 * Avoids flooding by using K_MIN_TIME_BETWEEN_EMAILS_MIN, will return false if too soon.
	 */
	public boolean sendEmailAvoidingFlood(String title, String text) {
		if (System.currentTimeMillis() - lastEmailAlarm < K_MIN_TIME_BETWEEN_EMAILS_MIN*60*1000 || silentMode == true) {
			// Too early to send one more email
			return false;
		}
		
		try {           
			// Get WAN IP so that a Browser can access EV3 as Webserver
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			String myWanIp = in.readLine(); //you get the IP as a String
			
			socket = new Socket("176.58.103.10", port); 		// mail.smtp2go.com

			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			DataInputStream isDI = new DataInputStream(socket.getInputStream());
			BufferedReader is = new BufferedReader(new InputStreamReader(isDI));
			
			// dos.writeBytes("EHLO "+ InetAddress.getLocalHost().getHostName() + "\r\n");
			dos.writeBytes("EHLO 10.0.0.1\r\n");
			dos.writeBytes("AUTH LOGIN");
			dos.writeBytes("\r\n");
			dos.writeBytes(username);
			dos.writeBytes("\r\n");
			dos.writeBytes(password);
			dos.writeBytes("\r\n");
			dos.writeBytes("MAIL FROM:<ev3.siva@gmail.com>\r\n");
			dos.writeBytes("\r\n");
			dos.writeBytes("RCPT TO: <frederic.siva@gmail.com>\r\n");
			dos.writeBytes("DATA\r\n");
			dos.writeBytes("Subject:" + title +"\r\n");
			dos.writeBytes("Message from EV3 (WAN IP=" + myWanIp + "):" + text);
			dos.writeBytes("\r\n.\r\n");
			dos.writeBytes("QUIT\r\n");
			dos.flush();

			theController.log("sending email Title [" + title + "]", 2);

			responseline = is.readLine();

			is.close();
			dos.close( );                  
			socket.close( );

			lastEmailAlarm = System.currentTimeMillis();
			
			return true;
		} 
		catch (IOException ex) {
			theController.log("Error sending email ", ex);
			return false;
		}
	}

	public void raiseAbnormalSensor(String descrS) {
		theController.log("Abnormal Temp Sensor behaviour: " + descrS, 1);
	}
	
	/**
	 * Will send alarm when temp conditions go crazy, but also triggers the water pump when reaching target temp to smoothen the variations.
	 */
	public void run() {
		long lastFeedCoolingScrew = 0;
		int screwTemp;
		boolean previousAlarmSituation = false; 
		int originalTraceLevel;
		
		theController.log("Alarm start running", 2);
		originalTraceLevel = theController.traceLevel;
		
		// TODO: set alarmingWaterT correctly
		
		while(pleaseStop == false) {
			// activating the circulator when any kind of Temp alarm is triggered, since it will always help to cool down the burner. 
			if (	newAlarmingSituation() == true && 
					previousAlarmSituation == false) {

					theController.log(
							"Alarming Situation (One temperature is too high, or a meter is broken : " + 
								(alarmingScrewT ? "ScrewAlarm" : "") + 
								( alarmingWaterT? " WaterAlarm" : "") + 
								( alarmingExhT? " ExhaustAlarm"  : "") + 
								( warningScrewT? " Screw Warning" : "") + 
							"),  turn water pump (Circulator) ON", 1);
					
					theController.getWPump().start();
					theController.getHeatingPump().start();
					previousAlarmSituation = true;
				
			} else if (	previousAlarmSituation == true && 
						newAlarmingSituation() == false ){
				
				theController.log("No more Alarming Situation, turn water pump (Circulator) OFF", 3);
				previousAlarmSituation = false;
				theController.getWPump().stop();
				
				// Heating Pump will be switched-off by the Controller.RefreshTemps() when Thermostat indicates it.
			}

			if (	newAlarmingSituation() == true && 
					theController.getWPump().isOn() == false) {

				theController.log("Weird Situation: The WP should be ON but is not, so activate Now", 0);
				theController.getWPump().start();

			}
			
			// Refresh all temps (During Idle, temps are only refreshed every 5 minutes...)
			theController.refreshTempValuesAndTriggerHeatingPump();
			
			// 1. Check water temp versus target  (Warning only, not a big deal)
			if (theController.getStoredWTemp() > theController.getTargetWTemp()+ deltaTempTriggerECS+K_HYSTERESIS && 
					alarmingWaterT == false && 
					theController.getTargetWTemp() != 0) {

				theController.log("Water temp is higher than target+" + deltaTempTriggerECS + ", set alarmingWaterT ON", 2);
				alarmingWaterT = true;
			} else if (theController.getStoredWTemp() <= theController.getTargetWTemp()+ deltaTempTriggerECS && alarmingWaterT == true) {

				theController.log("Water temp is no so high versus target+" + deltaTempTriggerECS + ", set alarmingWaterT off", 3);
				alarmingWaterT = false;
				
			}
			
			// 2. Compare Exh temp with Water temp (Warning only, not a big deal)

			if (theController.getStoredExhTemp() >=  waterPumpTriggerGazTemp+K_HYSTERESIS && alarmingExhT == false) {
				/*
				 *  Useful when Verner starts despite house not requesting hot water. (Gaz temp will rise very quickly, and water will follow...).
				 */
				theController.log("Relatively high gaz temp (" + (waterPumpTriggerGazTemp+K_HYSTERESIS) + 
						"), Set alarmingExhaustTemp ON (Must trigger WP in a few secs)", 2);
				alarmingExhT = true;
			} else if (theController.getStoredExhTemp() <  waterPumpTriggerGazTemp && alarmingExhT == true) {
				
					theController.log("Exh temp is no so high versus target " + (waterPumpTriggerGazTemp) + ", set alarmingExhT OFF", 3);
					alarmingExhT = false;				
			}
			
			// 3. Check water temp versus Max, BIG PROBLEM.
			if (theController.getStoredWTemp() >=  maxTempEau) {
				
				// This will force the Controller to cool-down
				theController.setTargetWTemp(0);	
				theController.getScreen().displayStatus("ALARM T EAU", false);

				alarmingWaterT = true;		// Will be set off during check 1.

				sendEmailAvoidingFlood(	"Verner Alarm Water" , 
										"Alarm T Water now=" + theController.getStoredWTemp() + 
										" Max=" + maxTempEau + 
										" . Will force Temp of 50 deg to water\n\n" +
										theController.statusHelper.getLatestEventsString());

				if (silentMode == false)
					beep();
			}

			// 4. Check Exhaust temp.
			if (theController.getStoredExhTemp() >=  maxTempGaz) {
				
				// This will force the Controller to cool-down
				theController.setTargetWTemp(0); 
				theController.getScreen().displayStatus("ALARM T GAZ", false);

				alarmingExhT = true;		// Will be set off during check 2.

				sendEmailAvoidingFlood(	"Verner Alarm Gaz Temp" , 
						"Alarm T Gaz now=" + theController.getStoredExhTemp() +
						" Max=" + maxTempGaz +
						" .Will shut boiler down (0 Deg)");

				if (silentMode == false)
					beep();
			} 

			screwTemp = theController.getScrewTemp();

			// 5. Warning for high temp on Screw (Alarm ony effective if 5 degs higher)
			if (screwTemp >  warningScrewTemp) {
				warningScrewT = true;
				
				if (System.currentTimeMillis() - lastFeedCoolingScrew > K_FEED_TEMP_ALARM_VIS_MIN*60*1000) {
					lastFeedCoolingScrew = System.currentTimeMillis();
					theController.getScreen().displayStatus("WARNING T VIS", false);

					theController.log("Warning: high screw temp " +  screwTemp + "deg (Trigger = " + (warningScrewTemp) + 
							" and 5deg more for Alarm), will feed for 3s and recheck after " + K_FEED_TEMP_ALARM_VIS_MIN + " min", 1);

					theController.getConveyor().feedForSeconds(3);				// Eject burning pellets in screw

					// Grating to move fire away
					theController.getConveyor().oneGratingCycle(true);			// Grating with refeed, to avoid pushing back fire in Silo

					// Re-read the temp
					screwTemp = theController.getScrewTemp();	// Update (On 10 Oct 2019, screwTemp was 46 where the log header was saying 30, how can that be?)
					
					// Alarming Screw Temp
					if (screwTemp >  maxScrewTemp) {
						theController.getScreen().displayStatus("ALARM T VIS", false);
						alarmingScrewT = true;
						
						// This will force the Controller to cool-down, not the case for a the simple warning above.
						theController.setTargetWTemp(0); 

						theController.log("Alarm: high screw temp " +  screwTemp + "deg (Trigger = " + (maxScrewTemp) + "), stop Boiler, set trace level to 4", 0);
						theController.traceLevel = 4;  // Increase trace level to have more debug info
						
						sendEmailAvoidingFlood(	"Verner Alarm T Vis", 
												"Alarm T Vis now=" + screwTemp + 
												" Max=" + maxScrewTemp +
												", feed tempo=" + K_FEED_TEMP_ALARM_VIS_MIN + "min.  More emails will be sent at that tempo if temp is still high.");
					} else if (alarmingScrewT == true){
						alarmingScrewT = false;
						theController.log("No more alarming screw temp: " +  screwTemp + "deg now (Alarm Trigger = " + (maxScrewTemp) + ")", 3);
					}
				}
				if (silentMode == false)
					beep();
			} else {
				// Screw temp is lower or equal to the Warning trigger point.
				if (warningScrewT == true) {
					// Just leaving the warning zone, still wait to drop 2 deg lower as hysteresis
					if (screwTemp < warningScrewTemp - hystereris) {
						warningScrewT = false;
						alarmingScrewT = false;	// Just in case temp dropped straight from Alarming temp to normal temp (Can happen with bad temp readings)

						theController.traceLevel = originalTraceLevel;	// Restore normal trace level

						theController.log("No more warning screw temp: " +  screwTemp + "deg now (Warning Trigger = " + (warningScrewTemp) + ")", 3);
					}
				} 
			}

	
			// 6. Detect wrong temp readings
			// TODO: Test this by unplugging the Arduino board from EV3.
			if (theController.getStoredWTemp() <=  0 && silentMode == false ) {
				
				// Water temp sensor must be broken, better to stop fire right now and go to afterburner
				theController.setTargetWTemp(0);	
				theController.getScreen().displayStatus("ALARM W SENSOR", false);

				sendEmailAvoidingFlood(	"Verner Alarm Water Sensor seems not working" , 
										"Alarm T Water now=" + theController.getStoredWTemp() + 
										" Max=" + maxTempEau + 
										" . Will force Temp of 50 deg to water");

				alarmingWaterT = true;

				while (theController.getState() != 5) {
					theController.skipCurrentPhase();
					beep();
					Delay.msDelay(5*1000);
				}
				
				if (silentMode == false)
					beep();
			}

			// 7. Monitor low water temp, to alert about Burner switched off  (No alarm if Exhaust temps are high, meaning fire is ON)
			if (theController.getStoredWTemp() < 40 && 
				silentMode == false && 
				theController.getStoredExhTemp() < 90) {
				if (silentMode == false)
					beepLoud();
				theController.log("Low water temp, beep: " +  theController.getStoredWTemp(), 3);

			}
			
			Delay.msDelay(tempoWaitS*1000);

		}
		

	}
}
