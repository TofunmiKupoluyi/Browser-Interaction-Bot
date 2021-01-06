package com.tkupoluyi.selenium_browser_test;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChromeDevToolsUtil {
    ChromeDriver driver;

    ChromeDevToolsUtil(ChromeDriver driver) {
        this.driver = driver;
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

    private String getListenerScriptSource(String scriptId) {
        driver.executeCdpCommand("Debugger.enable", new HashMap());
        Map query = new HashMap()
        {{
            put("scriptId", scriptId);
        }};
        Map result = driver.executeCdpCommand("Debugger.getScriptSource", query);
        return (String) result.get("scriptSource");
    }

    public Map convertNodeIdToObject(Long nodeId) {
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

    public ArrayList getListenersFromObjectId(String objectId) {
        Map query = new HashMap()
        {{
            put("objectId", objectId);
        }};
        Map result = driver.executeCdpCommand("DOMDebugger.getEventListeners", query);
        return (ArrayList) result.get("listeners");
    }

    public ArrayList<Long> getAllElementsNodeIds() {
        Long nodeIdOfBody = convertObjectIdToNodeId(getObjectIdOfElementByQuerySelector("*"));
        Map query = new HashMap()
        {{
            put("nodeId", nodeIdOfBody);
            put("selector", "*");
        }};
        Map result = driver.executeCdpCommand("DOM.querySelectorAll", query);
        return (ArrayList<Long>) result.get("nodeIds");
    }

    public Map getNodeDescription(Long nodeId) {
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

    public String generateXPathFromNodeDescription(Map nodeDescription) {
        String xpath = "//";
        ArrayList<String> attributes = (ArrayList<String>) nodeDescription.get("attributes");
        String tagName = ((String) nodeDescription.get("nodeName")).toLowerCase();
        boolean hasAttributes = false;
        xpath += tagName;
        for (int i=0; i < attributes.size(); i+=2) {
            if (!attributes.get(i).equals("style")) {
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
}
