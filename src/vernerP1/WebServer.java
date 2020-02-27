package vernerP1;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class WebServer extends NanoHTTPD implements Runnable {
	public VernerCtrl142 theController;

	public WebServer(VernerCtrl142 aController) {
        super(80, aController);
		theController = aController;
    }

	public void run() {
		try {
			super.start();
		} catch (IOException ex) {
			theController.log("Exception starting the Webserver", ex);;
		}
	}
	
    @Override public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        StringBuilder msg;
        
        // theController.log("Method received by Webserver =" + method + " '" + uri + "' ", 4);
        
        Map<String, String> parms = session.getParms();

        /**
        Map<String, String> header = session.getHeaders();

        Iterator<String> e = header.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            theController.log("  HDR: '" + value + "' = '" + header.get(value) + "'", 3);
        }

        e = parms.keySet().iterator();       
        theController.log(parms.keySet().size() + " parameters specified", 3);
        
        while (e.hasNext()) {
            String value = e.next();
            theController.log("  PRM: '" + value + "' = '" + parms.get(value) + "'", 3);
        }
		*/
        
        if (uri.equals("/big")) {
            msg = new StringBuilder(
            		"<html><head><title>Verner EVO F&F</title>" + 
            				// "<meta http-equiv=\"refresh\" content=\"60\">" + 
            		"</head>" + 
            		"<body><h2>EV3 Server Big" + theController.getVersion() + " <br/></h2>" + 
            		
            		theController.statusHelper.getBigHtmlString() + 
            		"<br/>");
        	
        } else if (uri.equals("/status")) {
            msg = new StringBuilder(
            		"{"+
					    "\"targetHeatingCoolingState\": " + theController.getHomebridgeString() + ",\n" +
					    "\"targetTemperature\":" + theController.getTargetWTemp() + ",\n" +
					    "\"currentHeatingCoolingState\": " + theController.getHomebridgeString() +",\n" +
					    "\"currentTemperature\": " + theController.getStoredWTemp() + "}");
            
            return new NanoHTTPD.Response(msg.toString());
        } else {
	        msg = new StringBuilder(
	        		"<html><head><title>Verner EVO F&F</title>" + 
	        				// "<meta http-equiv=\"refresh\" content=\"60\">" + 
	        		"</head>" + 
	        		"<body><h1>EV3 Server " + theController.getVersion() + " <br/>" + 
	        		theController.statusHelper.getVerboseStatusHtmlString() + 
	        		"</h1><br/>");
        }
        
        msg.append("<form action='?' method='get'>\n");
        msg.append("<p>Set Target Temp (or WPON WPOFF): <input type='text' name='cmd'></p>\n");
        msg.append("</form>\n");
        msg.append("</body></html>\n");

        if (parms.get("cmd") != null) {
        	theController.log("Web Command Received: " + parms.get("cmd"), 2);
        	
        	if (parms.get("cmd").equals("WPON")) {
				theController.getWPump().start();
        		
        	} else if (parms.get("cmd").equals("WPOFF")) {
    				theController.getWPump().stop();            		
        	} else if (parms.get("cmd").equals("FORCEQUIT")) {
    				theController.forceQuit(); // Dangerous since no more monitoring of temperatures!            		
            } else {
	        	int newTemp = Integer.parseInt(parms.get("cmd"));
	        	theController.log("No valid cmd received, will parse it as a new target temp ", 2);
	        	
	        	if (newTemp >= 0 && newTemp < 85)
	        		theController.setTargetWTemp(newTemp);
        	}
        }

        return new NanoHTTPD.Response(msg.toString());
    }
}
