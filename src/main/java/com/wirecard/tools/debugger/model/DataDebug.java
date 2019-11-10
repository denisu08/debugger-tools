package com.wirecard.tools.debugger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String cpb;

    private Map<String, List<Map>> brColl;
    private List<Map> currentBrColl; // UI always ignore this properties, only for server sync
    private Map<String, Map<String, ChainingBreakpointRequest>> breakpointEvents;

    private Map sysVar;
    private Map custVar;

    @JsonIgnore
    private JDIScript jdiScript;

    public DataDebug() {
        this.brColl = new HashMap<>();
        this.breakpointEvents = new HashMap<>();
        this.clear();
    }

    public String getCpb() {
        return cpb;
    }

    public void setCpb(String cpb) {
        this.cpb = cpb;
    }

    public List<Map> getCurrentBrColl() {
        return currentBrColl;
    }

    public void setCurrentBrColl(List<Map> currentBrColl) {
        this.currentBrColl = currentBrColl;
    }

    public Map<String, List<Map>> getBrColl() {
        return brColl;
    }

    public void setBrColl(Map<String, List<Map>> brColl) {
        this.brColl = brColl;
    }

    public Map<String, ChainingBreakpointRequest> getBreakpointEvents(String functionId) {
        return breakpointEvents.getOrDefault(functionId, new HashMap<>());
    }

    public void addBreakpointEvents(String functionId, String breakpointKey, ChainingBreakpointRequest chainingBreakpointRequest) {
        Map<String, ChainingBreakpointRequest> chainingBreakpointRequestList = this.breakpointEvents.get(functionId);
        if (chainingBreakpointRequestList == null) {
            chainingBreakpointRequestList = new HashMap();
        }
        chainingBreakpointRequestList.put(breakpointKey, chainingBreakpointRequest);
        this.breakpointEvents.put(functionId, chainingBreakpointRequestList);
    }

    public List<Map> getBrColl(String key) {
        if (brColl.containsKey(key)) {
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

    public void clearAndDisconnect() {
        this.clear();
        try {
            if (this.jdiScript != null || this.jdiScript.vm().process().isAlive()) getJdiScript().vm().dispose();
        } catch (Exception ex) {}
        // this.getJdiScript().vm().resume();
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
