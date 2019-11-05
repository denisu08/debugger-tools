package com.wirecard.tools.debugger.controller;

import com.wirecard.tools.debugger.model.DebugMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static java.lang.String.format;

@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection.");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String stageId = (String) headerAccessor.getSessionAttributes().get("stage_id");
        String serviceId = (String) headerAccessor.getSessionAttributes().get("service_id");
        if (stageId != null) {
            logger.info("StageId Disconnected: " + stageId);

            DebugMessage debugMessage = new DebugMessage();
            debugMessage.setType(DebugMessage.CommandType.DISCONNECT);
            debugMessage.setStageId(stageId);

            messagingTemplate.convertAndSend(format("/channel/%s", serviceId), debugMessage);
        }
    }
}
