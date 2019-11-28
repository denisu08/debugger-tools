package com.wirecard.tools.debugger.common;

import java.util.HashMap;
import java.util.Map;

public class WMSLink {
    private int id;
    private int from;
    private int to;
    private String text;
    private Map additionalInfo;

    public WMSLink(int id, int from, int to, String text, String... additionalInfo) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.text = text;
        this.additionalInfo = new HashMap();
        if (additionalInfo != null) {
            for (int i = 0; i < additionalInfo.length; i += 2) {
                this.additionalInfo.put(additionalInfo[i], additionalInfo[i + 1]);
            }
        }

        System.out.println(toString());
    }

    public Map convertToMap() {
        Map map = new HashMap();
        map.put("id", this.getId());
        map.put("from", this.getFrom());
        map.put("tp", this.getTo());
        map.put("text", this.getText());
        if (!this.additionalInfo.isEmpty()) {
            for (Object key : this.additionalInfo.keySet()) {
                map.put(key, this.additionalInfo.get(key));
            }
        }
        return map;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String toString() {
        return "\t Link(" + id + ") " + from + " - " + to + " \'" + text + "\' " + additionalInfo;
    }
}
