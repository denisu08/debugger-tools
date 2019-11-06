package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.example.ExampleConstant;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;
import com.wirecard.tools.debugger.model.DataDebug;
import com.wirecard.tools.debugger.model.DebugMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.unchecked;
import static java.lang.String.format;

@Controller
public class DebuggerController {

    private static final Logger logger = LoggerFactory.getLogger(DebuggerController.class);
    private Map<String, DataDebug> jdiContainer;
    private ObjectMapper om;

    private final SimpMessagingTemplate messagingTemplate;
    private int counter = 0;

    public DebuggerController(SimpMessagingTemplate _template) {
        this.messagingTemplate = _template;
        this.jdiContainer = new HashMap();
        this.om = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
    }

    @MessageMapping("/debugger/{serviceId}")
    public void attach(@DestinationVariable String serviceId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) throws JsonProcessingException {

        DataDebug dataDebug = jdiContainer.get(serviceId);
        String plainContent = new String(Base64.getDecoder().decode(debugMessage.getContent()));
        DataDebug dataDebugFromClient =  this.om.readValue(plainContent, DataDebug.class);

        switch (debugMessage.getType()) {
            case CONNECT:
                logger.info("attach: " + debugMessage);
                if (dataDebug == null) {
                    // need to set ip & port
                    dataDebug = dataDebugFromClient;

                    String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
                    String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);
                    JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());
                    j.onFieldAccess("com.wirecard.tools.debugger.jdiscript.example.HelloWorld", "helloTo", e -> {
                        j.onStepInto(e.thread(), j.once(se -> {
                            unchecked(() -> e.object().setValue(e.field(),
                                    j.vm().mirrorOf("JDIScript!")));
                        }));
                    });
                    dataDebug.setJdiScript(j);
                }

                dataDebug.setConnect(true);
                jdiContainer.put(serviceId, dataDebug);
                break;
            case DISCONNECT:
                logger.info("detached: " + debugMessage);

                // remove & clean dataDebug from specific key
                dataDebug.clearAndDisconnect();
                jdiContainer.remove(serviceId);
                break;
            case NEXT:
                logger.info("next: " + debugMessage);
                dataDebug.setClb(dataDebug.getClb() + 1);
                dataDebug.getJdiScript().run();
                break;
            case RESUME:
                logger.info("resume: " + debugMessage);
                counter = 0;
                break;
            case SET_BREAKPOINT:
                logger.info("set breakpoint: " + debugMessage);
                dataDebug.setBrColl(dataDebugFromClient.getBrColl());
            default:
                /*String currentRoomId = (String) headerAccessor.getSessionAttributes().put("service_id", serviceId);
                if (currentRoomId != null) {
                    DebugMessage leaveMessage = new DebugMessage();
                    leaveMessage.setType(DebugMessage.CommandType.SET_BREAKPOINT);
                    leaveMessage.setStageId(debugMessage.getStageId());
                    messagingTemplate.convertAndSend(format("/debug-channel/%s", currentRoomId), leaveMessage);
                }
                headerAccessor.getSessionAttributes().put("stageId", debugMessage.getStageId());
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), debugMessage);*/
                break;
        }

        messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(dataDebug));
    }
}
