package vernerP1;

import lejos.hardware.Button;
import lejos.hardware.lcd.*;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.NXTLightSensor;
import lejos.utility.Delay;
import java.net.URL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * 
 * API key obtained through rapidAPI (Using Google Account).  Free, only allows 100 queries per day.
 * Doc on https://openweathermap.org
 *
 */
public class OpenWeatherApp {
	String MyAPIKey = "3107464ed0mshfdc571c8007b6d3p19e875jsn28cfec578eba";
	String URLSTRING_TEMP = "https://community-open-weather-map.p.rapidapi.com/weather?id=2797714&units=metric&mode=xml&q=Namur,Belgium";

    URL url;
    URLConnection urlConnection = null;
    HttpURLConnection connection = null;
    BufferedReader in = null;
    String urlContent = "";

    float storedTempF = -128;
    
	VernerCtrl142 theController;
	TempCollector theRecorder;
	
	public OpenWeatherApp(VernerCtrl142 aController) {
		theController = aController;
		theRecorder = new TempCollector(aController, "Ext");	
	}

    private String getWebServerAnswer(String urlString) throws IOException, IllegalArgumentException {
        String urlStringCont = "";
    
        // creating URL object
        url = new URL(urlString);
        urlConnection = url.openConnection();
        connection = null;

        // we can check, if connection is proper type
        if (urlConnection instanceof HttpURLConnection) {
            connection = (HttpURLConnection) urlConnection;
            connection.setRequestProperty("x-rapidapi-host", "community-open-weather-map.p.rapidapi.com");
            connection.setRequestProperty("x-rapidapi-key", MyAPIKey);
            
        } else {
        	theController.log("Invalid HTTP URL to Get Ext Temp [" + urlString + "]", 1);
            throw new IOException("HTTP URL is not correct");
        }
    
        // we can check response code (200 OK is expected)
        // System.out.println("Response Code=" + connection.getResponseCode() + ", MSG=" + connection.getResponseMessage());
        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        
        String current;

        while ((current = in.readLine()) != null) {
            urlStringCont += current;
        }
        
        in.close();
        
        return urlStringCont;
    }

    public float getCachedLocalTemp() {
    	return storedTempF;
    }

    /**
     * 
     * May take up to 30 seconds for whole call!
     */
    public float getLocalTempFromOpenWeatherApp() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String xmlResponseS;

        if (true) 
        	return -1;	// For Testing
        
        try 
        {
        	xmlResponseS = getWebServerAnswer(URLSTRING_TEMP);

        	final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document document= builder.parse(new InputSource(new StringReader(xmlResponseS)));
			
			final Element racine = document.getDocumentElement();
			final Element temp = (Element) racine.getElementsByTagName("temperature").item(0);
			  
			storedTempF = Float.parseFloat(temp.getAttribute("value"));

        	theController.log("answer Weather from Web Server [" + storedTempF + "]", 2);

 
    	  return storedTempF;
        } catch (Exception e) {
        	theController.log("Error getting answer Weather from Web Server [" + URLSTRING_TEMP + "]", e);
        }		
        
        storedTempF = -127;
        return -127;	// Error code
    }
    
}

