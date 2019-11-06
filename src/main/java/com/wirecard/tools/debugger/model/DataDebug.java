package com.wirecard.tools.debugger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wirecard.tools.debugger.jdiscript.JDIScript;

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
    private List brColl;
    private Map sysVar;
    private Map custVar;

    @JsonIgnore
    private JDIScript jdiScript;

    public DataDebug() {
        this.brColl = new ArrayList<>();
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

    public List getBrColl() {
        return brColl;
    }

    public void setBrColl(List brColl) {
        this.brColl = brColl;
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
        try {
            if (this.jdiScript != null || this.jdiScript.vm().process().isAlive()) getJdiScript().vm().exit(0);
        } catch (Exception ex) {}
        this.jdiScript = null;
    }

    @Override
    public String toString() {
        return "DataDebug{" +
                "ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", connect=" + connect +
                ", mute=" + mute +
                ", clb='" + clb + '\'' +
                ", brColl='" + brColl + '\'' +
                ", sysVar=" + sysVar +
                ", custVar=" + custVar +
                ", jdiScript=" + jdiScript +
                '}';
    }
}
