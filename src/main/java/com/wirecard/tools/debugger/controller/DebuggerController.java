package com.wirecard.tools.debugger.controller;

import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.example.ExampleConstant;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;
import com.wirecard.tools.debugger.model.DebugMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.unchecked;
import static java.lang.String.format;

@Controller
public class DebuggerController {

    private static final Logger logger = LoggerFactory.getLogger(DebuggerController.class);

    private final SimpMessagingTemplate messagingTemplate;

    private int counter = 0;

    public DebuggerController(SimpMessagingTemplate _template) {
        this.messagingTemplate = _template;
    }

    public static void main(final String[] args) {

        String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
        String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);

        JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());

        j.onFieldAccess("com.wirecard.tools.debugger.jdiscript.example.HelloWorld", "helloTo", e -> {
            j.onStepInto(e.thread(), j.once(se -> {
                unchecked(() -> e.object().setValue(e.field(),
                        j.vm().mirrorOf("JDIScript!")));
            }));
        });

        j.run();
    }

    @MessageMapping("/debugger/{serviceId}")
    public void attach(@DestinationVariable String serviceId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) {
        switch (debugMessage.getType()) {
            case CONNECT:
                logger.info("attach: " + debugMessage);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), "is_connect::true");
                break;
            case DISCONNECT:
                logger.info("detached: " + debugMessage);
                counter = 0;
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), format("is_connect::#clb::%s)", counter));
                break;
            case NEXT:
                logger.info("next: " + debugMessage);
                counter++;
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), format("is_connect::#clb::%s)", counter));
                break;
            case RESUME:
                logger.info("resume: " + debugMessage);
                counter = 0;
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), format("is_connect::#clb::%s)", counter));
                break;
            case SET_BREAKPOINT:
                String currentRoomId = (String) headerAccessor.getSessionAttributes().put("service_id", serviceId);
                if (currentRoomId != null) {
                    DebugMessage leaveMessage = new DebugMessage();
                    leaveMessage.setType(DebugMessage.CommandType.SET_BREAKPOINT);
                    leaveMessage.setStageId(debugMessage.getStageId());
                    messagingTemplate.convertAndSend(format("/debug-channel/%s", currentRoomId), leaveMessage);
                }
                headerAccessor.getSessionAttributes().put("stageId", debugMessage.getStageId());
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), debugMessage);
            default:
                break;
        }
    }
}
