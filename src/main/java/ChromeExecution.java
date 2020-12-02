import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChromeExecution {
    ChromeDriver driver;
    String url;
    Map<String, ArrayList<Map>> xpathListenersMap;
    FileOutputStream outputFile;
    String outputFileDirectory;
    boolean persistToFile;
    long startTimeMillis;
    ChromeExecution(String url) {
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
//        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200","--ignore-certificate-errors");
        this.driver = new ChromeDriver(options);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.persistToFile = false;
        startTimeMillis = (new Date()).getTime();
    }

    ChromeExecution(String url, String outputFileDirectory) {
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        this.driver = new ChromeDriver(options);
        this.url = url;
        this.xpathListenersMap = new HashMap<>();
        this.outputFileDirectory = outputFileDirectory;
        this.outputFile = null;
        this.persistToFile = true;
    }

    private String getObjectIdOfElementByQuerySelector(String element) {
        Map query = new HashMap()
        {{
            put("expression", "document.querySelector('"+element+"')" );
        }};
        Map result = driver.executeCdpCommand("Runtime.evaluate", query);
        return (String)((Map)result.get("result")).get("objectId");
    }

    private Long convertObjectIdToNodeId(String objectId) {
        Map query = new HashMap()
        {{
            put("objectId", objectId );
        }};
        Map result = driver.executeCdpCommand("DOM.requestNode", query);
        return (Long) result.get("nodeId");
    }

    private Map convertNodeIdToObject(Long nodeId) {
        Map query = new HashMap()
        {{
            put("nodeId", nodeId );
        }};
        try {
            Map result = driver.executeCdpCommand("DOM.resolveNode", query);
            return (Map) result.get("object");
        } catch (WebDriverException ex) {
            System.out.println("No node with node id, " + nodeId);
            return null;
        }
    }

    private ArrayList getListenersFromObjectId(String objectId) {
        Map query = new HashMap()
        {{
            put("objectId", objectId);
        }};
        Map result = driver.executeCdpCommand("DOMDebugger.getEventListeners", query);
        return (ArrayList) result.get("listeners");
    }

    private void logListener(Map listener) {
        System.out.println(listener);
        System.out.println("Listener type: "+ (String) listener.get("type"));
        System.out.println("Listener Source: ");
        String scriptId = (String)listener.get("scriptId");
        int columnNumber = ((Long)listener.get("columnNumber")).intValue();
        int lineNumber = ((Long)listener.get("lineNumber")).intValue();
        String listenerSource = getListenerScriptSource(scriptId);
        String[] listenerSourceLines = listenerSource.split("\\n");
        if (listenerSourceLines.length >= lineNumber) {
            System.out.println(listenerSourceLines[lineNumber].substring(columnNumber));
        }
    }

    private ArrayList<Long> getAllElementsNodeIds() {
        Long nodeIdOfBody = convertObjectIdToNodeId(getObjectIdOfElementByQuerySelector("*"));
        Map query = new HashMap()
        {{
            put("nodeId", nodeIdOfBody);
            put("selector", "*");
        }};
        Map result = driver.executeCdpCommand("DOM.querySelectorAll", query);
        return (ArrayList<Long>) result.get("nodeIds");
    }

    private Map getNodeDescription(Long nodeId) {
        Map query = new HashMap()
        {{
            put("nodeId", nodeId);
        }};
        try {
            Map result = driver.executeCdpCommand("DOM.describeNode", query);
            return (Map) result.get("node");
        } catch (Exception ex) {
            return null;
        }
    }

    private String generateXPathFromNodeDescription(Map nodeDescription) {
        String xpath = "//";
        ArrayList<String> attributes = (ArrayList<String>) nodeDescription.get("attributes");
        String tagName = ((String) nodeDescription.get("nodeName")).toLowerCase();
        boolean hasAttributes = false;
        xpath += tagName;
        for (int i=0; i < attributes.size(); i+=2) {
            if (attributes.get(i) != "style") {
                if (!hasAttributes) {
                    hasAttributes = true;
                    xpath += "[";
                }  else {
                    xpath += " and ";
                }
                xpath += "@"+ attributes.get(i) + "='" + attributes.get(i+1) + "'";
            }
        }
        if (hasAttributes) {
            xpath += "]";
        }
        return xpath;
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

    private void implicitWaitForNodeIdSearch() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (Exception ex) {

        }
    }

    private String getListenerScriptSource(String scriptId) {
        driver.executeCdpCommand("Debugger.enable", new HashMap());
        Map query = new HashMap()
        {{
            put("scriptId", scriptId);
        }};
        Map result = driver.executeCdpCommand("Debugger.getScriptSource", query);
        return (String) result.get("scriptSource");
    }


    private void interactWithAllElements() {
        this.xpathListenersMap.forEach(this::triggerListenersOnElementByXPath);
    }

    private void triggerListenersOnElementByXPath(String xpath, ArrayList<Map> listeners) {
        System.out.println(xpath);
        System.out.println(listeners);
        try {
            WebElement element = driver.findElement(By.xpath(xpath));
            listeners.forEach((listener) -> {
                triggerListener(element, listener);
//                waitForPageLoad();
                if (!driver.getCurrentUrl().equals(url)) {
                    System.out.println("Page change happened");
                    openPage();
                }
            });
        } catch(NoSuchElementException ex) {
            System.out.println("Can't find element");
            // TODO: Implement retry strategy if we can't find element that has listeners
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
                actions.moveToElement(element).click(element).build().perform();
            } else if (listenerType.equals("mouseover") || listenerType.equals("mouseenter")) {
                System.out.println("mouseover initiated");
                actions.moveToElement(element).build().perform();
            } else if (listenerType.equals("mouseout") || listenerType.equals("mouseleave")) {
                System.out.println("mouseout initiated");
                actions.moveToElement(element).build().perform();
                actions.moveByOffset(100,100).build().perform();
            } else if (listenerType.equals("keydown") || listenerType.equals("keypress") || listenerType.equals("keyup")) {
                System.out.println("keydown initiated");
                actions.moveToElement(element).click(element).sendKeys("ABCD").perform();
            } else if (listenerType.equals("dblclick")) {
                System.out.println("dblclick initiated");
                actions.moveToElement(element).doubleClick(element).perform();
            } else if (listenerType.equals("load")) {
                System.out.println("load initiated");
            } else if (listenerType.equals("change")) {
                System.out.println("change initiated");
            } else if (listenerType.equals("drag") || listenerType.equals("dragstart") || listenerType.equals("dragend")) {
                // TODO: Add remaining drag functionality
                System.out.println("drag initiated");
                actions.moveToElement(element).dragAndDropBy(element, 60,0).perform();
//                actions.moveToElement(element).dragAndDropBy(element, 0,200).perform();
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
                outputFile = new FileOutputStream(outputFileDirectory);
            }
            outputFile.write(log.getBytes());
        } catch (FileNotFoundException ex) {
            System.out.println("File could not be found");
        } catch (IOException ex) {
            System.out.println("Error writing to file");
        }
    }

    private void closeTools() {
        driver.close();
        if (outputFile != null) {
            try {
                outputFile.flush();
                outputFile.close();
            } catch(IOException ex) {
                System.out.println("Error occured while closing stream");
            }
        }
    }

    public void execute() {
        openPage();
        implicitWaitForNodeIdSearch();
        ArrayList<Long> nodeIds = getAllElementsNodeIds();
        nodeIds.forEach((Long nodeId) -> {
            Map nodeIdToObject = convertNodeIdToObject(nodeId);
            if (nodeIdToObject != null) {
                String nodeObjectId = (String) nodeIdToObject.get("objectId");
                ArrayList<Map> listeners = getListenersFromObjectId(nodeObjectId);
                Map nodeDescription = getNodeDescription(nodeId);
                if (nodeDescription != null) {
                    String xpath = generateXPathFromNodeDescription(nodeDescription);
                    xpathListenersMap.put(xpath, listeners); // Assemble all the xpaths first before triggering
                }
            }
        });
        interactWithAllElements();
        collectLogs();
        System.out.println("Execution time: " + ((new Date()).getTime() - startTimeMillis));
        driver.close();
    }

}
