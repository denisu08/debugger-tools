package com.wirecard.tools.debugger.common;

import java.util.HashMap;
import java.util.Map;

public class WMSNode {

    public static final int KEY_NODE_START = -1;
    public static final int KEY_NODE_END = -2;

    public static WMSNode createStartNode() {
        return new WMSNode(WMSNode.KEY_NODE_START, "Start", "category", "Start");
    }

    public static WMSNode createEndNode() {
        return new WMSNode(WMSNode.KEY_NODE_END, "End", "category", "End");
    }

    public WMSNode(int key, String name) {
        this(key, name, new String[]{});
    }

    public WMSNode(int key, String name, String... additionalInfo) {
        this.key = key;
        this.text = name;
        this.additionalInfo = new HashMap();
        if (additionalInfo != null) {
            for (int i = 0; i < additionalInfo.length; i += 2) {
                this.additionalInfo.put(additionalInfo[i], additionalInfo[i + 1]);
            }
        }
        System.out.println(toString());
    }

    private int key;
    private String text;
    private Map additionalInfo;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
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
        return "\tNode => " +
                "key=" + key +
                ", name='" + text + '\'' +
                ", additionalInfo=" + additionalInfo;
    }
}
