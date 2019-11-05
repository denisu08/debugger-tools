package com.wirecard.tools.debugger.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirecard.tools.debugger.jdiscript.JDIScript;

import java.util.HashMap;
import java.util.Map;

public class DataDebug {

    private String ip = "";
    private String port = "";
    private boolean connect = false;
    private boolean mute = false;
    private String clb = "";
    private String brColl = "";
    private Map sysVar;
    private Map custVar;
    private JDIScript jdiScript;

    public DataDebug() {
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

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
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

    public String getClb() {
        return clb;
    }

    public void setClb(String clb) {
        this.clb = clb;
    }

    public String getBrColl() {
        return brColl;
    }

    public void setBrColl(String brColl) {
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
