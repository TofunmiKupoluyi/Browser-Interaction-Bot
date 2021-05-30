package com.tkupoluyi.browser_interaction_bot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String eventType;
    private String xpath;
    private List<Event> predecessorEvents;
    private List<Event> children;

    Event(String eventType, String xpath) {
        this.eventType = eventType;
        this.xpath = xpath;
        predecessorEvents = new ArrayList<>();
        children = new ArrayList<Event>();
    }

    public void setPredecessorEvents(List<Event> predecessorEvents) {
        this.predecessorEvents = predecessorEvents;
    }

    public String getEventType() {
        return eventType;
    }

    public String getXpath() {
        return xpath;
    }

    public List<Event> getPredecessorEvents() {
        return predecessorEvents;
    }

    public void addChild(Event childEvent) {
        children.add(childEvent);
        childEvent.predecessorEvents.addAll(predecessorEvents);
        childEvent.predecessorEvents.add(this);
    }

    public JSONObject serializeEvent() { ;
        JSONObject obj = new JSONObject();
        obj.put("event", eventType);
        obj.put("xpath", xpath);
        return obj;
    }

    public JSONArray serializeFullEventTrace() {
        JSONArray returnArray = new JSONArray();
        for(Event predecessor: predecessorEvents) {
            returnArray.add(predecessor.serializeEvent());
        }
        returnArray.add(serializeEvent());
        return returnArray;
    }

    public String generateDOTString() {
        return "\""+ xpath + " | " + eventType + "\"";
    }

    public String generateFullDOTRepresentation() {
        StringBuilder returnStringBuilder = new StringBuilder();
        for(Event predecessor: predecessorEvents) {
            returnStringBuilder.append(predecessor.generateDOTString());
            returnStringBuilder.append("->");
        }
        returnStringBuilder.append(generateDOTString());
        return returnStringBuilder.toString();
    }
}
