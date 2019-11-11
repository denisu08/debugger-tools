package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirecard.tools.debugger.common.DebuggerUtils;
import com.wirecard.tools.debugger.common.GlobalVariables;
import com.wirecard.tools.debugger.model.DebugMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Component
public class DebuggerSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(DebuggerSocketEventListener.class);

    private ObjectMapper om = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        logger.info("Received a new web socket connection." + headerAccessor);

        /*
        GenericMessage genericMessage = (GenericMessage) headerAccessor.getMessageHeaders().get("simpConnectMessage");
        Map<String, List<String>> multiValueMap = genericMessage.getHeaders().get(StompHeaderAccessor.NATIVE_HEADERS, Map.class);
        if (multiValueMap.containsKey("serviceId") && GlobalVariables.jdiContainer.containsKey(multiValueMap.get("serviceId").get(0))) {
            try {
                String serviceId = multiValueMap.get("serviceId").get(0);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
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
            // debugMessage.setStageId(stageId);

            messagingTemplate.convertAndSend(format("/channel/%s", serviceId), debugMessage);
        }
    }
}
