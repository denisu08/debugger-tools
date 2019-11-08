package com.wirecard.tools.debugger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.jdi.event.BreakpointEvent;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataDebug {

    private String ip;
    private int port;
    private boolean connect;
    private boolean mute;
    private int clb;

    private Map<String, List<Map>> brColl;
    private Map<String, List<ChainingBreakpointRequest>> breakpointEvents;

    private Map sysVar;
    private Map custVar;

    @JsonIgnore
    private JDIScript jdiScript;

    public DataDebug() {
        this.brColl = new HashMap<>();
        this.breakpointEvents = new HashMap<>();
        this.clear();
    }

    public void clear() {
        this.ip = "";
        this.port = 0;
        this.connect = false;
        this.mute = false;
        this.clb = 0;
        this.sysVar = new HashMap();
        this.custVar = new HashMap();
        this.breakpointEvents.clear();
        if (brColl != null) {
            for (String key : brColl.keySet()) {
                for (Map br : brColl.get(key)) {
                    br.put("isDebug", false);
                }
            }
        }
    }

    public List<ChainingBreakpointRequest> getBreakpointEvents(String key) {
        if(breakpointEvents.containsKey(key)) {
            return breakpointEvents.get(key);
        }
        return null;
    }

    public void addBreakpointEvents(String key, ChainingBreakpointRequest chainingBreakpointRequest) {
        List<ChainingBreakpointRequest> chainingBreakpointRequestList = this.breakpointEvents.get(key);
        if(chainingBreakpointRequestList == null) {
            chainingBreakpointRequestList = new ArrayList<>();
        }
        chainingBreakpointRequestList.add(chainingBreakpointRequest);
        this.breakpointEvents.put(key, chainingBreakpointRequestList);
    }

    public List<Map> getBrColl(String key) {
        if(brColl.containsKey(key)) {
            return brColl.get(key);
        }
        return null;
    }

    public void putBrColl(String key, List<Map> brCollections) {
        this.brColl.put(key, brCollections);
    }

    public JDIScript getJdiScript() {
        return jdiScript;
    }

    public void setJdiScript(JDIScript jdiScript) {
        this.jdiScript = jdiScript;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }


    public boolean isConnect() {
        return connect;
    }

    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getClb() {
        return clb;
    }

    public void setClb(int clb) {
        this.clb = clb;
    }

    public Map getSysVar() {
        return sysVar;
    }

    public void setSysVar(Map sysVar) {
        this.sysVar = sysVar;
    }

    public Map getCustVar() {
        return custVar;
    }

    public void setCustVar(Map custVar) {
        this.custVar = custVar;
    }

    public void clearAndDisconnect() {
        this.clear();
//        try {
//            if (this.jdiScript != null || this.jdiScript.vm().process().isAlive()) getJdiScript().vm().exit(0);
//        } catch (Exception ex) {}
        this.getJdiScript().vm().resume();
        this.jdiScript = null;
    }

    @Override
    public String toString() {
        return "DataDebug{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", connect=" + connect +
                ", mute=" + mute +
                ", clb=" + clb +
                ", brColl=" + brColl +
                ", breakpointEvents=" + breakpointEvents +
                ", sysVar=" + sysVar +
                ", custVar=" + custVar +
                ", jdiScript=" + jdiScript +
                '}';
    }
}
