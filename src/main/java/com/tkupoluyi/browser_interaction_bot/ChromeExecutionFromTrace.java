package com.tkupoluyi.browser_interaction_bot;

import com.tkupoluyi.browser_interaction_bot.event_handling.EventHandler;
import com.tkupoluyi.browser_interaction_bot.exceptions.InteractionBotException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ChromeExecutionFromTrace extends ChromeExecution {
    String traceFileName;

    ChromeExecutionFromTrace(String url, EventHandler eventHandler, String traceFileName) {
        super(url, eventHandler);
        this.outputFileDirectory = "screenshots_trace";
        this.traceFileName = traceFileName;
    }

    ChromeExecutionFromTrace(String url, EventHandler eventHandler, String outputFileDirectory, String traceFileName) {
        super(url, eventHandler, outputFileDirectory);
        this.traceFileName = traceFileName;
    }

    ChromeExecutionFromTrace(String url, EventHandler eventHandler, String outputFileDirectory, String proxyUrl, String traceFileName) {
        super(url, eventHandler, outputFileDirectory, proxyUrl);
        this.traceFileName = traceFileName;
    }

    public Event execute() {
        try {
            FileReader fileReader = new FileReader(traceFileName);
            BufferedReader br = new BufferedReader(fileReader);
            String line;
            JSONParser parser = new JSONParser();
            while((line=br.readLine())!=null) {
                this.url = BrowserInteractions.openPage(driver, url);
                BrowserInteractions.scrollToTop(driver);
                JSONArray arr = (JSONArray) parser.parse(line);
                int i;
                for (i = 0; i < arr.size(); i++) {
                    JSONObject json = (JSONObject) arr.get(i);
                    System.out.println((String) json.get("event") + " " + (String) json.get("xpath"));
                    Event event = new Event((String) json.get("event"), (String) json.get("xpath"));
                    try {
                        eventHandler.triggerEvent(event);
                    } catch (InteractionBotException ex) {System.out.println(ex.getExceptionType()); }
                }
                screenshot();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {

        } catch (ParseException e) {
            e.printStackTrace();
        }
        closeTools();
        return null;
    }
}
