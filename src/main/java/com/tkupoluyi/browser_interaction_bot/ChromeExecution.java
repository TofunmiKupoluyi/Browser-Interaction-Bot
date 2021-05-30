package com.tkupoluyi.browser_interaction_bot;

import com.tkupoluyi.browser_interaction_bot.event_handling.EventHandler;
import com.tkupoluyi.browser_interaction_bot.exceptions.InteractionBotException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.*;

public class ChromeExecution {
    ChromeDriver driver;
    String url;
    FileOutputStream outputFile;
    String outputFileDirectory = "screenshots";
    FileWriter traceFileWriter;
    ChromeOptions chromeOptions;
    ChromeDriverService chromeDriverService;
    boolean persistToFile;
    long startTimeMillis;
    int screenshotCount = 0;
    DOTFileBuilder dotFileBuilder;
    EventHandler eventHandler;

    ChromeExecution(String url, EventHandler eventHandler) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.persistToFile = false;
        this.startTimeMillis = (new Date()).getTime();
        this.eventHandler = eventHandler;
        this.eventHandler.setDriver(driver);
        dotFileBuilder = new DOTFileBuilder(outputFileDirectory);
    }

    ChromeExecution(String url, EventHandler eventHandler, String outputFileDirectory) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.outputFileDirectory = outputFileDirectory;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.outputFile = null;
        this.eventHandler = eventHandler;
        this.eventHandler.setDriver(driver);
        dotFileBuilder = new DOTFileBuilder(outputFileDirectory);
    }

    ChromeExecution(String url, EventHandler eventHandler, String outputFileDirectory, String proxyUrl) {
        setDefaultChromeOptions();
        chromeOptions.addArguments("--proxy-server="+proxyUrl);
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.eventHandler = eventHandler;
        this.eventHandler.setDriver(driver);
        dotFileBuilder = new DOTFileBuilder(outputFileDirectory);
    }

    private void setDefaultChromeOptions() {
        Map<String, Object> prefs = new HashMap<String, Object>();
        Map<String, Object> mobileEmulation = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
//        mobileEmulation.put("deviceName", "Nexus 5");
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("prefs", prefs);
//        chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-popup-blocking");
//        chromeOptions.addArguments("--headless");
        chromeDriverService = ChromeDriverService.createDefaultService();
    }

    protected void screenshot() {
        this.screenshotCount +=1;
        BrowserInteractions.screenshot(driver, outputFileDirectory + "/" + this.screenshotCount);
    }

    private void writeToTraceFile(String eventString) {
        try {
            if (traceFileWriter == null) {
                traceFileWriter = new FileWriter(outputFileDirectory + "/" + "trace");
            }
            traceFileWriter.write(eventString+"\n");
            traceFileWriter.flush();

        } catch (IOException ex) {
            System.out.println("Error writing to trace file");

        }
    }

    protected void closeTools() {
        driver.close();
        if (traceFileWriter != null) {
            try {
                traceFileWriter.close();
            } catch(IOException ignored) {}
        }
        dotFileBuilder.close();
    }

    public Event execute() throws InteractionBotException {
        this.url = BrowserInteractions.openPage(driver, url);
        HTMLDocumentUtil htmlDocumentUtil = new HTMLDocumentUtil(driver);
        LinkedList<Event> eventList = htmlDocumentUtil.getEventList();
        System.out.println("No of events: "+eventList.size());
        Event baseEvent = new Event("baseEvent", "/html/body");
        Queue<Event> eventQueue = new LinkedList<>();
        eventQueue.add(baseEvent);

        while (!eventQueue.isEmpty()) {
            // Check if this is a leaf for dot file purposes
            boolean hasChild = false;

            BrowserInteractions.openPage(driver, url);
            BrowserInteractions.scrollToTop(driver);
            Event parentEvent = eventQueue.poll();

            // Trigger all predecessors and then trigger the element
            for (Event predecessor: parentEvent.getPredecessorEvents()) {
                eventHandler.triggerEvent(predecessor);
            }
            eventHandler.triggerEvent(parentEvent);

            screenshot();
            writeToTraceFile(parentEvent.serializeFullEventTrace().toJSONString());

            // Do proper check to see if the url has change, consider # changes in url
            if (!driver.getCurrentUrl().equals(url)) {
                BrowserInteractions.openPage(driver, url);
                continue;
            }

            int i = eventList.size()-1;
            while (i >= 0) {
                Event event = eventList.get(i);
                System.out.println(event.getEventType()+" "+event.getXpath());
                try {
                    eventHandler.triggerEvent(event);
                    parentEvent.addChild(event);
                    eventQueue.add(event);
                    eventList.remove(i);
                    hasChild = true;
                    BrowserInteractions.openPage(driver, url);
                    for (Event predecessor: parentEvent.getPredecessorEvents()) {
                        eventHandler.triggerEvent(predecessor);
                    }
                    eventHandler.triggerEvent(parentEvent);
                } catch (InteractionBotException ex) {
                    if (ex.getExceptionType() == InteractionBotException.UNREACHEABLE_BROWSER) {
                        throw ex;
                    }

                    if (!driver.getCurrentUrl().equals(url)) {
                        BrowserInteractions.openPage(driver, url);
                        for (Event predecessor: parentEvent.getPredecessorEvents()) {
                            eventHandler.triggerEvent(predecessor);
                        }
                        eventHandler.triggerEvent(parentEvent);
                    }
                }
                i--;
            }
            // if it is a leaf, add path to dot file
            if (!hasChild) {
                dotFileBuilder.addNode(parentEvent.generateFullDOTRepresentation());
            }
        }

        for (Event i: eventList) {
            System.out.println(i.getEventType()+ " " + i.getXpath());
        }
        System.out.println("Complete");
        closeTools();
        return baseEvent;
    }
}
