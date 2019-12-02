package com.wirecard.tools.debugger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wirecard.tools.debugger.common.DebuggerConstant;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;

import java.util.*;

public class DataDebug {

    private String user;
    private String password;
    private String ip;
    private String logPath;
    private boolean listenLogger = false;
    private int port;
    private boolean connect;
    private boolean mute;
    private int clb;
    private String cpb;
    private Map sysVar;
    private Map<String, String> custVar;

    private List<Map> currentBrColl; // UI always ignore this properties, only for server sync
    private Map<String, List<Map>> brColl;

    @JsonIgnore
    private Map<String, Map<String, ChainingBreakpointRequest>> breakpointEvents;

    @JsonIgnore
    private JDIScript jdiScript;

    public DataDebug() {
        this.user = "";
        this.brColl = new HashMap<>();
        this.breakpointEvents = new HashMap<>();
        this.clear();
    }

    public boolean isListenLogger() {
        return listenLogger;
    }

    public void setListenLogger(boolean listenLogger) {
        this.listenLogger = listenLogger;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Set<String> getBrClasses() {
        Set<String> result = new HashSet<>();
        if (this.brColl != null) {
            for (String key : this.brColl.keySet()) {
                result.add(key.split(DebuggerConstant.DEBUGGER_FORMAT_PARAM)[0].trim());
            }
        }
        return result;
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

    public Map<String, String> getCustVar() {
        return custVar;
    }

    public void setCustVar(Map<String, String> custVar) {
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

    public void disconnect() {
        this.connect = false;
        this.clb = 0;
        this.cpb = "xx";
        this.breakpointEvents.clear();
        try {
            if (this.jdiScript != null || this.jdiScript.vm().process().isAlive()) getJdiScript().vm().dispose();
        } catch (Exception ex) {
        }
        // this.getJdiScript().vm().resume();
        this.jdiScript = null;
    }

    @Override
    public String toString() {
        return "DataDebug{" +
                "ip='" + ip + '\'' +
                ", user=" + user +
                ", password=" + password +
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
