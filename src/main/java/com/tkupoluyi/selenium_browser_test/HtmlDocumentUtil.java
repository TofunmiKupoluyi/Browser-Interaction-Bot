package com.tkupoluyi.selenium_browser_test;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.jsoup.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HtmlDocumentUtil {

    private Document doc;
    private ArrayList<String> xpathList;
    private Map<String, ArrayList<Map>> xpathListenerMap;
    private ChromeDriver driver;
    HtmlDocumentUtil(ChromeDriver driver) {
        this.doc = Jsoup.parse(driver.getPageSource());
        this.driver = driver;
        this.xpathList = new ArrayList<>();
        this.xpathListenerMap = new HashMap<>();
        dfs(doc.body(), "/html/body");
    }

    private void dfs(Element root, String xpath) {
        this.xpathList.add(xpath);
        getEventListenersByXpath(xpath);
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i< root.childrenSize(); i++) {
            Element child = root.child(i);
            String tagName = child.tagName();
            if (!tagName.equals("link") && !tagName.equals("script") && !tagName.equals("style")) {
                int currTagIndex = map.getOrDefault(tagName, 0) + 1;
                map.put(tagName, currTagIndex);
                if (child.attributes().hasKey("id")) {
                    dfs(child, xpath + "/" + tagName + "[@id='" + child.attributes().get("id") + "']");
                } else if (child.attributes().hasKey("class")) {
                    dfs(child, xpath + "/" + tagName + "[@class='" + child.attributes().get("class") + "']");
                } else {
                    dfs(child, xpath + "/" + tagName + "[" + currTagIndex + "]");
                }
            }
        }
    }

    private void getEventListenersByXpath(String xpath) {
        Map query = new HashMap()
        {{
            put("expression", "document.evaluate(\""+xpath+"\",document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null).snapshotItem(0)");
        }};
        try {
            Map result = (Map) driver.executeCdpCommand("Runtime.evaluate", query).get("result");
            String objectId = (String) result.get("objectId");
            ArrayList<Map> listeners = getEventListenersByObjectId(objectId);
            xpathListenerMap.put(xpath, listeners);
        } catch (WebDriverException ex) {
            System.out.println("Trouble locating xpath, " + xpath);
        }
    }

    private ArrayList<Map> getEventListenersByObjectId(String objectId) {
        Map query = new HashMap()
        {{
            put("objectId", objectId);
        }};
        Map result = driver.executeCdpCommand("DOMDebugger.getEventListeners", query);
        return (ArrayList) result.get("listeners");
    }

    public ArrayList<String> getXpathList() {
        return xpathList;
    }

    public Map<String, ArrayList<Map>> getXpathListenerMap() {
        return xpathListenerMap;
    }
}
