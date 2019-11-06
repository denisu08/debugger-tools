package com.wirecard.tools.debugger.model;

import java.util.Map;

public class DebugMessage {
    public enum CommandType {
        CONNECT, DISCONNECT, RESUME, NEXT, MUTE, SET_BREAKPOINT, ADD_VARIABLE, REMOVE_VARIABLE
    }

    private CommandType messageType;
    private String content;
    private String stageId;

    public CommandType getType() {
        return messageType;
    }

    public void setType(CommandType messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    @Override
    public String toString() {
        return "DebugMessage{" +
                "messageType=" + messageType +
                ", content=" + content +
                ", stageId='" + stageId + '\'' +
                '}';
    }
}
