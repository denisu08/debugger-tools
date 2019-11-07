package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.wirecard.tools.debugger.common.DebuggerUtils;
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
    public void attach(@DestinationVariable String serviceId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) throws Exception {

        DataDebug dataDebug = jdiContainer.get(serviceId);
        String plainContent = new String(Base64.getDecoder().decode(debugMessage.getContent()));
        DataDebug dataDebugFromClient = this.om.readValue(plainContent, DataDebug.class);
        boolean runCommand = true;

        // new jd.core.Decompiler().decompileClass("compiled.jar", "com/namespace/SomeClass.class");

        switch (debugMessage.getType()) {
            case CONNECT:
                logger.info("attach: " + debugMessage);
                if (dataDebug == null) {
                    // need to set ip & port
                    dataDebug = dataDebugFromClient;

                    String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
                    System.out.println("decompile:: " + DebuggerUtils.decompileCode("C:/Wirecard/CBB_Project/debugger-tools/HelloWorld.class"));
                    String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);
                    JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());
                    dataDebug.setJdiScript(j);
                    jdiContainer.put(serviceId, dataDebug);


//                    j.vmDeathRequest(event -> {
//                        finalDataDebug.clearAndDisconnect();
//                        jdiContainer.remove(serviceId);
//                    });
                    /*j.onFieldAccess("com.wirecard.tools.debugger.jdiscript.example.HelloWorld", "helloTo", e -> {
                        j.onStepInto(e.thread(), j.once(se -> {
                            // unchecked(() -> e.object().setValue(e.field(), j.vm().mirrorOf("JDIScript!")));
                            try {
                                List<Field> childFields = e.location().declaringType().allFields();
                                StackFrame stackFrame = e.thread().frame(0);
                                Map sysVar = new HashMap<>();
                                for (Field childField : childFields) {
                                    Value val = stackFrame.thisObject().getValue(childField);
                                    sysVar.put(childField.name(), Utils.getJavaValue(val));
                                }
                                finalDataDebug.setSysVar(sysVar);
                                finalDataDebug.setClb(1);
                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(finalDataDebug));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }));
                    });*/

                    Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName("startMe").stream()
                            .filter(m -> m.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE))
                            .forEach(m -> {
                                try {
                                    List<Location> locationList = m.allLineLocations();
                                    for (Location loc : locationList) {
                                        j.breakpointRequest(loc, be -> {
                                            System.out.println("be: " + be);
                                            try {
                                                // get field current class
                                                List<Field> childFields = m.location().declaringType().allFields();
                                                StackFrame stackFrame = be.thread().frame(0);
                                                Map sysVar = new HashMap<>();
                                                for (Field childField : childFields) {
                                                    Value val = stackFrame.thisObject().getValue(childField);
                                                    sysVar.put(childField.name(), DebuggerUtils.getJavaValue(val, be.thread()));
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

                                                jdiContainer.get(serviceId).setSysVar(sysVar);
                                                jdiContainer.get(serviceId).setClb(1);
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(jdiContainer.get(serviceId)));
                                                j.vm().suspend();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                            jdiContainer.get(serviceId).getBreakpointEvents().add(be);
                                        }).setEnabled(true);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
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
                }

                jdiContainer.get(serviceId).setConnect(true);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(jdiContainer.get(serviceId)));
                dataDebug.getJdiScript().run();
                runCommand = false;
                break;
            case DISCONNECT:
                logger.info("detached: " + debugMessage);

                // remove & clean dataDebug from specific key
                dataDebug.clearAndDisconnect();
                jdiContainer.remove(serviceId);
                break;
            case NEXT:
                logger.info("next: " + debugMessage);
                // dataDebug.setClb(dataDebug.getClb() + 1);
                dataDebug.getJdiScript().vm().resume();
                break;
            case RESUME:
                logger.info("resume: " + debugMessage);
                dataDebug.getJdiScript().vm().resume();
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

        if (runCommand) {
            messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(dataDebug));
        }
    }
}
