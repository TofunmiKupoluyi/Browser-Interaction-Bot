package com.tkupoluyi.selenium_browser_test;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.io.*;
import java.util.*;

public class ChromeExecution extends Thread {
    ChromeDriver driver;
    String url;
    Map<String, ArrayList<Map>> xpathListenersMap;
    Queue<Map<String, Object>> retryQueue;
    FileOutputStream outputFile;
    String outputFileDirectory;
    ChromeOptions chromeOptions;
    ChromeDriverService chromeDriverService;
    Long processId;
    boolean persistToFile;
    long startTimeMillis;
    int screenshotCount = 0;
    boolean isForward = true;
    boolean isMac = System.getProperty("os.name").equals("Mac OS X");
    boolean isError = false;
    boolean isTimeout = false;
    int unreachableCnt = 0;
    int timeLimit = 60000*15;

    ChromeExecution(String url) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.persistToFile = false;
        this.startTimeMillis = (new Date()).getTime();
        this.retryQueue = new LinkedList<>();
    }

    ChromeExecution(String url, String outputFileDirectory) {
        setDefaultChromeOptions();
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.outputFile = null;
    }

    ChromeExecution(String url, String outputFileDirectory, String proxyUrl) {
        setDefaultChromeOptions();
        chromeOptions.addArguments("--proxy-server="+proxyUrl);
        chromeOptions.addArguments("--headless");
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.retryQueue = new LinkedList<>();
    }

    ChromeExecution(String url, String outputFileDirectory, String proxyUrl, String extensionDir) {
        setDefaultChromeOptions();
        chromeOptions.addExtensions(new File(extensionDir));
        chromeOptions.addArguments("--proxy-server="+proxyUrl);
        this.driver = new ChromeDriver(chromeDriverService, chromeOptions);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
        this.startTimeMillis = (new Date()).getTime();
        this.retryQueue = new LinkedList<>();
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
//        chromeOptions.addArguments("--headless");
        chromeDriverService = ChromeDriverService.createDefaultService();

    }

    private void openPage() {
        driver.get(url);
        waitForPageLoad();
        this.url = driver.getCurrentUrl(); // This sets url to what it is when page has loaded
    }

    private void getProcessId() {
        try {
            if (processId == null) {
                String[] cmd = {"/bin/bash", "-c", "lsof -ti:" + Integer.toString(chromeDriverService.getUrl().getPort()) + ""};
                Process pb = Runtime.getRuntime().exec(cmd);
                String line;
                BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
                long rootPid = ProcessHandle.current().pid();
                while ((line = input.readLine()) != null) {
                    if (Long.parseLong(line) != rootPid) {
                        System.out.println("THIS IS THE PROCESS ID: " + line);
                        processId = Long.parseLong(line);
                    }
                }
                input.close();
            }
        } catch (IOException ignore) {
            System.out.println("ERROR GETTING PROCESS: "+ ignore.getMessage());
        }
    }

    private void killProcess() {
        try {
            if (processId != null) {
                System.out.println("WE ARE KILLING A PROCESS");
                String[] cmd = {"/bin/bash","-c","kill -9 "+ processId + ""};
                Process pb = Runtime.getRuntime().exec(cmd);
                String line;
                BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                }
                input.close();
            }
        } catch (IOException ignore) {
            System.out.println("ERROR KILLING PROCESS: "+ ignore.getMessage());
        }
    }
    private void waitForPageLoad() {
        String loadState = driver.executeScript("return document.readyState").toString();
        while (!loadState.equals("complete")) {
            loadState = driver.executeScript("return document.readyState").toString();
        }
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
        Map element;
        int startSize = retryQueue.size();
        for (int i=0; i< startSize; i++) {
            element = retryQueue.poll();
            String xpath = (String) element.get("xpath");
            ArrayList<Map> listeners = (ArrayList<Map>) element.get("listeners");
            triggerListenersOnElementByXPath(xpath, listeners, true);
        }
    }

    private void screenshot(String id) {
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            if (isForward) {
                FileUtils.copyFile(scrFile, new File(id + ".png"));
            } else {
                FileUtils.copyFile(scrFile, new File(id + "_backward.png"));
            }
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
            try {
                String originalHandle = driver.getWindowHandle();
                for (String handle : driver.getWindowHandles()) {
                    if (!handle.equals(originalHandle)) {
                        driver.switchTo().window(handle);
                        driver.close();
                    }
                }
                driver.switchTo().window(originalHandle);
            } catch (NoSuchWindowException ex) {
                System.out.println("Window already closed");
            }
        }
    }

    private void screenshotNewTab(String id) {
        try {
            String originalHandle = driver.getWindowHandle();
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(originalHandle)) {
                    driver.switchTo().window(handle);
                    waitForPageLoad();
                    screenshot(id);
                    this.screenshotCount += 1;
                    driver.close();
                }
            }
            driver.switchTo().window(originalHandle);
        } catch (NoSuchWindowException ex) {
            System.out.println("Window already closed");
        }

    }


    private boolean triggerListenersOnElementByXPath(String xpath, ArrayList<Map> listeners, boolean... stopRetry) {
        try {
            WebElement element = driver.findElement(By.xpath(xpath)); // It is at this point that a NoSuchElementException is triggered
            listeners.forEach((listener) -> {
                triggerListener(element, listener, xpath);
                closeExtraneousTabs(5);
                if (checkPageChange()) {
                    openPage();
                }
                this.screenshotCount+=1;
            });
            if (stopRetry.length == 0) {
                retryInteractableElements();
            }
            unreachableCnt = 0;
            return true;
        }
        catch (NotFoundException | StaleElementReferenceException ex) {
            if (listeners.size() > 0) {
                Map retryMap = new HashMap();
                retryMap.put("xpath", xpath);
                retryMap.put("listeners", listeners);
                retryQueue.add(retryMap);
            }
            return true;
        }
        catch (UnreachableBrowserException ex) {
            unreachableCnt += 1;
            if (unreachableCnt < 30) {
                System.out.println("Browser Unreachable. Retrying in 1 second, "+(unreachableCnt)+"/30");
                try { sleep(1000); } catch (InterruptedException ignore) { }
                return false;
            }
            throw ex;
        }
    }

    private void triggerListener(WebElement element, Map listener, String xpath) {
        try {
            String listenerType = (String) listener.get("type");
            Keys controlKey = isMac ? Keys.COMMAND : Keys.CONTROL;
            Actions actions = new Actions(driver);
            if (listenerType.equals("click") || listenerType.equals("mousedown") || listenerType.equals("mouseup") || listenerType.equals("focus") || listenerType.equals("blur")) {
                actions.moveToElement(element).keyDown(controlKey).click(element).keyUp(controlKey).build().perform();
                actions.moveToElement(element).keyDown(controlKey).click(element).keyUp(controlKey).build().perform();
                actions.moveToElement(element).keyDown(controlKey).click(element).keyUp(controlKey).build().perform();
                screenshotNewTab("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("mouseover") || listenerType.equals("mouseenter")) {
                actions.moveToElement(element).build().perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("mouseout") || listenerType.equals("mouseleave")) {
                actions.moveToElement(element).build().perform();
                actions.moveByOffset(100, 100).build().perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("keydown") || listenerType.equals("keypress") || listenerType.equals("keyup") || listenerType.equals("input")) {
                actions.moveToElement(element).click(element).sendKeys("ABCD").build().perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("dblclick")) {
                actions.moveToElement(element).keyDown(controlKey).doubleClick(element).keyUp(controlKey).build().perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("load")) {
            } else if (listenerType.equals("change")) {
                actions.moveToElement(element).click(element).sendKeys("ABCD").build().perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
            } else if (listenerType.equals("drag") || listenerType.equals("dragstart") || listenerType.equals("dragend")) {
                actions.moveToElement(element).dragAndDropBy(element, 100, 0).perform();
                screenshot("xpath_"+xpath+"_"+listenerType);
//                actions.moveToElement(element).dragAndDropBy(element, 0,60).perform();
            } else {
                System.out.println("Unhandled event: " + listenerType);
            }
        } catch(MoveTargetOutOfBoundsException | JavascriptException | NullPointerException ignored) { }

    }

    private void collectLogs() {
        if (persistToFile && outputFileDirectory != null) {
            LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
            List<LogEntry> logs= entry.getAll();
            logs.forEach((log) -> {
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
        if (outputFile != null) {
            try {
                outputFile.flush();
                outputFile.close();
            } catch(IOException ex) {
                System.out.println("Error occured while closing stream");
            }
        }

        try {
            driver.close();
            driver.quit();
        } catch (NoSuchWindowException ex) {
            System.out.println("Window already closed");
        }
    }

    private void iterateThroughXpathList(ArrayList<String> xpathList, Map<String, ArrayList<Map>> xpathListenerMap, int direction) {
        if (direction == 1) {
            int i = 0;
            while (i < xpathList.size()) {
                // Returns false if the browser is unreachable
                if ((new Date()).getTime() - startTimeMillis > timeLimit) {
                    throw new TimeoutException();
                }
                if (triggerListenersOnElementByXPath(xpathList.get(i), xpathListenerMap.getOrDefault(xpathList.get(i), new ArrayList<>()))) {
                    i++;
                }
            }
        } else {
            int i = xpathList.size()-1;
            while (i >= 0) {
                // Returns false if the browser is unreachable
                if ((new Date()).getTime() - startTimeMillis > timeLimit) {
                    throw new TimeoutException();
                }
                if (triggerListenersOnElementByXPath(xpathList.get(i), xpathListenerMap.getOrDefault(xpathList.get(i), new ArrayList<>()))) {
                    i--;
                }
            }
        }
    }

    public void runForward() {
        ArrayList<String> xpathList;
        HtmlDocumentUtil htmlDocumentUtil;
        int reloadCnt = 0;
        do {
            openPage();
            htmlDocumentUtil = new HtmlDocumentUtil(driver);
            xpathList = htmlDocumentUtil.getXpathList();
            reloadCnt += 1;
        } while (xpathList.size() <= 1 && reloadCnt <= 10);
        getProcessId();
        Map<String, ArrayList<Map>> xpathListenerMap = htmlDocumentUtil.getXpathListenerMap();
        scrollToBottom();
        scrollToTop();
        iterateThroughXpathList(xpathList, xpathListenerMap, 1);
        openPage();
        retryInteractableElements();
        collectLogs();
        closeTools();
    }

    public void runBackward() {
        ArrayList<String> xpathList;
        HtmlDocumentUtil htmlDocumentUtil;
        int reloadCnt = 0;
        do {
            openPage();
            htmlDocumentUtil = new HtmlDocumentUtil(driver);
            xpathList = htmlDocumentUtil.getXpathList();
            reloadCnt+=1;
        } while (xpathList.size() <= 1 && reloadCnt <= 10);
        getProcessId();
        Map<String, ArrayList<Map>> xpathListenerMap = htmlDocumentUtil.getXpathListenerMap();
        scrollToBottom();
        scrollToTop();
        iterateThroughXpathList(xpathList, xpathListenerMap, -1);
        openPage();
        retryInteractableElements();
        collectLogs();
        closeTools();
    }

    public void setBackward() {
        isForward = false;
    }

    @Override
    public void run() {
        if (isForward) {
            try {
                runForward();
            } catch(Exception ex) {
                System.out.println("Fatal error occurred in forward thread: "+ex.getClass());
                isError = true;
                if (ex.getClass() == TimeoutException.class) {
                    isTimeout = true;
                }
                try {
                    collectLogs();
                    closeTools();
                } catch(Exception ignored) { }
            } finally {
                getProcessId();
                killProcess();
                interrupt();
            }
        }
        else {
            try {
                runBackward();
            } catch(Exception ex) {
                System.out.println("Fatal error occurred in backward thread: "+ex.getClass());
                isError = true;
                if (ex.getClass() == TimeoutException.class) {
                    isTimeout = true;
                }
                try {
                    collectLogs();
                    closeTools();
                } catch (Exception ignored) { }
            } finally {
                getProcessId();
                killProcess();
                interrupt();
            }
        }
    }

}
