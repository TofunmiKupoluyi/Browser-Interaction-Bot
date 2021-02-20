package com.tkupoluyi.selenium_browser_test;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.seleniumhq.jetty9.util.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ChromeExecution extends Thread {
    ChromeDriver driver;
    String url;
    Map<String, ArrayList<Map>> xpathListenersMap;
    Queue<Map<String, Object>> retryQueue;
    FileOutputStream outputFile;
    String outputFileDirectory;
    ChromeOptions chromeOptions;
    boolean persistToFile;
    long startTimeMillis;
    int screenshotCount = 0;
    boolean isForward = true;

    ChromeExecution(String url) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.persistToFile = false;
        this.startTimeMillis = (new Date()).getTime();
        this.retryQueue = new LinkedList<>();
    }

    ChromeExecution(String url, String outputFileDirectory) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.outputFile = null;

    }

    ChromeExecution(String url, String outputFileDirectory, String extensionDir) {
        setDefaultChromeOptions();
        chromeOptions.addExtensions(new File(extensionDir));
        this.driver = new ChromeDriver(chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
        this.retryQueue = new LinkedList<>();
    }

    ChromeExecution(String url, String outputFileDirectory, String extensionDir, String proxyUrl) {
        setDefaultChromeOptions();
        chromeOptions.addExtensions(new File(extensionDir));
        chromeOptions.addArguments("--proxy-server="+proxyUrl);
        this.driver = new ChromeDriver(chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
        this.retryQueue = new LinkedList<>();
    }

    private void setDefaultChromeOptions() {
        Map<String, Object> prefs = new HashMap<String, Object>();
        Map<String, Object> mobileEmulation = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        mobileEmulation.put("deviceName", "Nexus 5");
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        chromeOptions.addArguments("--ignore-certificate-errors");
    }

    private void openPage() {
        driver.get(url);
        waitForPageLoad();
        this.url = driver.getCurrentUrl(); // This sets url to what it is when page has loaded
    }

    private void waitForPageLoad() {
        String loadState = driver.executeScript("return document.readyState").toString();
        while (!loadState.equals("complete")) {
            loadState = driver.executeScript("return document.readyState").toString();
        }
        System.out.println("Page load complete");
    }

    private boolean checkPageChange() {
        // This ensures page load while also ensuring that the page doesn't change
        String loadState = driver.executeScript("return document.readyState").toString();
        while (!loadState.equals("complete") && driver.getCurrentUrl().equals(this.url)) {
            loadState = driver.executeScript("return document.readyState").toString();
        }

        if (!driver.getCurrentUrl().equals(this.url)) {
            return true;
        }
        return false;
    }

    private void retryInteractableElements() {
        // If we can't find the element, we won't go in to retry other interactable elements, if we do find the element, we will recurse but behavior is good
        System.out.println("-----");
        Map element;
        int startSize = retryQueue.size();
        for (int i=0; i< startSize && (element = retryQueue.poll()) != null; i++) {
            String xpath = (String) element.get("xpath");
            ArrayList<Map> listeners = (ArrayList<Map>) element.get("listeners");
            System.out.println("Retrying: "+xpath);
            triggerListenersOnElementByXPath(xpath, listeners);
        }
    }

    private void screenshot(int cnt) {
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, new File(cnt+".png"));
        } catch (Exception IOException) {
            System.out.println("Error in screenshot");
        }
    }

    private void scrollToBottom() {
        driver.executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    private void scrollToTop() {
        driver.executeScript("window.scrollTo(0, 0)");
    }

    private void closeExtraneousTabs(int limit) {
        if (driver.getWindowHandles().size() < limit) {
            return;
        }
        else {
            String originalHandle = driver.getWindowHandle();
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(originalHandle)) {
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
            driver.switchTo().window(originalHandle);
        }

    }

    private void triggerListenersOnElementByXPath(String xpath, ArrayList<Map> listeners) {
        System.out.println(xpath);
        System.out.println(listeners);
        try {
            WebElement element = driver.findElement(By.xpath(xpath)); // It is at this point that a NoSuchElementException is triggered
            listeners.forEach((listener) -> {
                triggerListener(element, listener);
//                screenshot(this.screenshotCount);
                closeExtraneousTabs(10);
                if (checkPageChange()) {
                    System.out.println("Page change happened");
                    openPage();
                }
                this.screenshotCount+=1;
            });
            retryInteractableElements();
        } catch(NoSuchElementException ex) {
            if (listeners.size() > 0) {
                Map retryMap = new HashMap();
                retryMap.put("xpath", xpath);
                retryMap.put("listeners", listeners);
                retryQueue.add(retryMap);
                System.out.println("Can't find element, listeners exist, added to retry queue");
            }  else {
                System.out.println("Can't find element");
            }
        } catch(Exception ex) {
            System.out.println("Error occurred " + ex.getMessage());
        }
        System.out.println("-----");
    }

    private void triggerListener(WebElement element, Map listener) {
        String listenerType = (String) listener.get("type");
        Actions actions = new Actions(driver);
        try {
            if (listenerType.equals("click") || listenerType.equals("mousedown") || listenerType.equals("mouseup")) {
                System.out.println("click initiated");
                try {
                    actions.moveToElement(element).keyDown(Keys.COMMAND).click(element).keyUp(Keys.COMMAND).build().perform();
                    actions.moveToElement(element).keyDown(Keys.COMMAND).click(element).keyUp(Keys.COMMAND).build().perform();
                    actions.moveToElement(element).keyDown(Keys.COMMAND).click(element).keyUp(Keys.COMMAND).build().perform();
                } catch(Exception e) {

                }
            } else if (listenerType.equals("mouseover") || listenerType.equals("mouseenter")) {
                System.out.println("mouseover initiated");
                actions.moveToElement(element).build().perform();
            } else if (listenerType.equals("mouseout") || listenerType.equals("mouseleave")) {
                System.out.println("mouseout initiated");
                actions.moveToElement(element).build().perform();
                actions.moveByOffset(100,100).build().perform();
            } else if (listenerType.equals("keydown") || listenerType.equals("keypress") || listenerType.equals("keyup")) {
                System.out.println("keydown initiated");
                actions.moveToElement(element).click(element).sendKeys("ABCD").build().perform();
            } else if (listenerType.equals("dblclick")) {
                System.out.println("dblclick initiated");
                actions.moveToElement(element).keyDown(Keys.COMMAND).doubleClick(element).keyUp(Keys.COMMAND).build().perform();
            } else if (listenerType.equals("load")) {
                System.out.println("load initiated");
            } else if (listenerType.equals("change")) {
                System.out.println("change initiated");
            } else if (listenerType.equals("drag") || listenerType.equals("dragstart") || listenerType.equals("dragend")) {
                // TODO: Add remaining drag functionality, also add try/catch for each individual drag
                System.out.println("drag initiated");
                actions.moveToElement(element).dragAndDropBy(element, 200,0).perform();
//                actions.moveToElement(element).dragAndDropBy(element, 0,60).perform();
            }
            else {
                System.out.println("Unhandled event: "+ listenerType);
            }
        } catch(Exception ex) {
            System.out.println("An error occurred while interacting with element: "+ ex.getClass());
        }
    }

    private void collectLogs() {
        if (persistToFile && outputFileDirectory != null) {
            LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
            List<LogEntry> logs= entry.getAll();
            System.out.println("These were the logs recorded: "+ logs.size());
            logs.forEach((log) -> {
                System.out.println(log.getMessage());
                writeToFile(log.getMessage()+"\n");
            });
        }
    }

    private void writeToFile(String log) {
        try {
            if (outputFile == null) {
                outputFile = new FileOutputStream(outputFileDirectory, true);
            }
            outputFile.write(log.getBytes());
        } catch (FileNotFoundException ex) {
            System.out.println("File could not be found");
        } catch (IOException ex) {
            System.out.println("Error writing to file");
        }
    }

    private void closeTools() {
        closeExtraneousTabs(0);
        driver.close();
        driver.quit();
        if (outputFile != null) {
            try {
                outputFile.flush();
                outputFile.close();
            } catch(IOException ex) {
                System.out.println("Error occured while closing stream");
            }
        }
    }

    public void runForward() {
        openPage();
        HtmlDocumentUtil htmlDocumentUtil = new HtmlDocumentUtil(driver);
        ArrayList<String> xpathList = htmlDocumentUtil.getXpathList();
        Map<String, ArrayList<Map>> xpathListenerMap = htmlDocumentUtil.getXpathListenerMap();
        scrollToBottom();
        scrollToTop();

        xpathList.forEach((xpath) -> {
            triggerListenersOnElementByXPath(xpath, xpathListenerMap.getOrDefault(xpath, new ArrayList<>()));
        });

        openPage();
        retryInteractableElements();
        collectLogs();
        closeTools();
        System.out.println("Execution time: " + ((new Date()).getTime() - startTimeMillis));
    }

    public void runBackward() {
        openPage();
        HtmlDocumentUtil htmlDocumentUtil = new HtmlDocumentUtil(driver);
        ArrayList<String> xpathList = htmlDocumentUtil.getXpathList();
        Map<String, ArrayList<Map>> xpathListenerMap = htmlDocumentUtil.getXpathListenerMap();
        scrollToBottom();
        scrollToTop();

        for(int i = xpathList.size()-1; i >= 0; i--) {
            triggerListenersOnElementByXPath(xpathList.get(i), xpathListenerMap.getOrDefault(xpathList.get(i), new ArrayList<>()));
        }

        openPage();
        retryInteractableElements();
        collectLogs();
        closeTools();
        System.out.println("Execution time: " + ((new Date()).getTime() - startTimeMillis));
    }

    public void execute() {
        openPage();
        HtmlDocumentUtil htmlDocumentUtil = new HtmlDocumentUtil(driver);
        ArrayList<String> xpathList = htmlDocumentUtil.getXpathList();
        Map<String, ArrayList<Map>> xpathListenerMap = htmlDocumentUtil.getXpathListenerMap();

        xpathList.forEach((xpath) -> {
            this.triggerListenersOnElementByXPath(xpath, xpathListenerMap.getOrDefault(xpath, new ArrayList<>()));
        });

        collectLogs();
        System.out.println("Execution time: " + ((new Date()).getTime() - startTimeMillis));
        closeTools();
    }

    public void setBackward() {
        isForward = false;
    }

    @Override
    public void run() {
        if (isForward) {
            runForward();
        }   else {
            runBackward();
        }
    }

}
