package com.wirecard.tools.debugger.model;

public class DebugMessage {
    public enum CommandType {
        CONNECT, DISCONNECT, RESUME, NEXT, MUTE, SET_BREAKPOINT, ADD_VARIABLE, REMOVE_VARIABLE
    }

    private CommandType messageType;
    private String content;
    private String functionId;

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

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

    @Override
    public String toString() {
        return "DebugMessage{" +
                "messageType=" + messageType +
                ", content=" + content +
                ", functionId=" + functionId +
                '}';
    }
}
