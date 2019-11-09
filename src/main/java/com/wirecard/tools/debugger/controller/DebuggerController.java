package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.wirecard.tools.debugger.common.DebuggerUtils;
import com.wirecard.tools.debugger.common.GlobalVariables;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.example.ExampleConstant;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;

// https://itsallbinary.com/java-debug-interface-api-jdi-hello-world-example-programmatic-stepping-through-the-code-lines/
// https://dzone.com/articles/monitoring-classloading-jdi
// https://www.element84.com/blog/jdi-mind-tricks
// https://www.baeldung.com/java-debug-interface

@Controller
public class DebuggerController {

    private static final Logger logger = LoggerFactory.getLogger(DebuggerController.class);
    private ObjectMapper om;

    private final SimpMessagingTemplate messagingTemplate;

    public DebuggerController(SimpMessagingTemplate _template) {
        this.messagingTemplate = _template;
        this.om = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
    }

    @MessageMapping("/debugger/{serviceId}")
    public void attach(@DestinationVariable String serviceId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) throws Exception {

        logger.info(debugMessage.getType() + ": " + debugMessage);

        DataDebug dataDebug = GlobalVariables.jdiContainer.get(serviceId);
        String plainContent = new String(Base64.getDecoder().decode(debugMessage.getContent()));
        DataDebug dataDebugFromClient = this.om.readValue(plainContent, DataDebug.class);
        boolean runCommand = true;

        // if null, so client is pointed as latest data
        if (dataDebug == null) {
            dataDebug = dataDebugFromClient;
            GlobalVariables.jdiContainer.put(serviceId, dataDebug);
        }

        switch (debugMessage.getType()) {
            case CONNECT:
                // need to set ip & port
                String OPTIONS = ExampleConstant.CLASSPATH_FROM_JAR;
                Map<String, String> sourceMap = DebuggerUtils.getSourceMap(serviceId);
                System.out.println("decompile:: " + sourceMap);
                String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE_FROM_JAR);
                JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());
                GlobalVariables.jdiContainer.get(serviceId).setJdiScript(j);

                j.vmDeathRequest(event -> {
                    if (GlobalVariables.jdiContainer.containsKey(serviceId)) {
                        GlobalVariables.jdiContainer.get(serviceId).clearAndDisconnect();
                        GlobalVariables.jdiContainer.remove(serviceId);
                        DebuggerUtils.removeSourceMap(serviceId);
                    }
                });

                Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName("startMe").stream()
                        .filter(m -> m.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE_FROM_JAR))
                        .forEach(m -> {
                            if (GlobalVariables.jdiContainer.containsKey(serviceId)) {
                                try {
                                    List<Location> locationList = m.allLineLocations();

                                    // filter based on source code & stageList

                                    for (Location loc : locationList) {
                                        ChainingBreakpointRequest chainingBreakpointRequest = j.breakpointRequest(loc, be -> {
                                            System.out.println("be: " + be);
                                            try {
                                                // get field current class
                                                List<Field> childFields = m.location().declaringType().allFields();
                                                StackFrame stackFrame = be.thread().frame(0);
                                                Map sysVar = new HashMap<>();
                                                for (Field childField : childFields) {
                                                    if (!childField.isStatic()) {
                                                        Value val = stackFrame.thisObject().getValue(childField);
                                                        sysVar.put(childField.name(), DebuggerUtils.getJavaValue(val, be.thread()));
                                                    }
                                                }

                                                // get field local variable
                                                List<LocalVariable> localVariables = loc.method().variables();
                                                Map<String, LocalVariable> localVariableMap = new HashMap<>(localVariables.size());
                                                for (LocalVariable variable : localVariables) {
                                                    if (variable.isVisible(stackFrame)) {
                                                        Value val = stackFrame.getValue(variable);
                                                        sysVar.put(variable.name(), DebuggerUtils.getJavaValue(val, be.thread()));
                                                    }
                                                }

                                                GlobalVariables.jdiContainer.get(serviceId).setSysVar(sysVar);
                                                GlobalVariables.jdiContainer.get(serviceId).setClb(1);
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
                                                j.vm().suspend();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }).setEnabled(true);

                                        GlobalVariables.jdiContainer.get(serviceId).addBreakpointEvents(debugMessage.getFunctionId(), chainingBreakpointRequest);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });

                j.vm().allClasses().forEach(c -> setConstructBrks.accept(c));
                j.onClassPrep(cp -> setConstructBrks.accept(cp.referenceType()));

//                    j.onMethodInvocation("com.wirecard.tools.debugger.jdiscript.example.HelloWorld", "start", e -> {
//                        j.onStepInto(e.thread(), j.once(se -> {
//                            // unchecked(() -> e.object().setValue(e.field(), j.vm().mirrorOf("JDIScript!")));
//                            try {
//                                List<Field> childFields = e.location().declaringType().allFields();
//                                StackFrame stackFrame = e.thread().frame(0);
//                                Map sysVar = new HashMap<>();
//                                for (Field childField : childFields) {
//                                    Value val = stackFrame.thisObject().getValue(childField);
//                                    sysVar.put(childField.name(), Utils.getJavaValue(val));
//                                }
//                                finalDataDebug.setSysVar(sysVar);
//                                finalDataDebug.setClb(1);
//                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(finalDataDebug));
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
//                        }));
//                    });

                GlobalVariables.jdiContainer.get(serviceId).setConnect(true);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
                GlobalVariables.jdiContainer.get(serviceId).getJdiScript().run();
                runCommand = false;
                break;
            case DISCONNECT:
                // remove & clean dataDebug from specific key
                GlobalVariables.jdiContainer.get(serviceId).clearAndDisconnect();
                GlobalVariables.jdiContainer.remove(serviceId);
                break;
            case NEXT:
                GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vm().resume();
                break;
            case RESUME:
                GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vm().resume();
                break;
            case SET_BREAKPOINT:
                GlobalVariables.jdiContainer.get(serviceId).putBrColl(debugMessage.getFunctionId(), dataDebugFromClient.getCurrentBrColl());
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

        if (runCommand) {
            messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
        }
    }
}
