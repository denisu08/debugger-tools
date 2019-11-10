package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.sun.tools.jdi.LocationImpl;
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

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

                this.collectBreakpointEvents(serviceId, debugMessage.getFunctionId());

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

    private void collectBreakpointEvents(final String serviceId, final String functionId) {
        DataDebug dataDebug = GlobalVariables.jdiContainer.get(serviceId);
        JDIScript j = dataDebug.getJdiScript();

        Set<String> keySet = dataDebug.getBrColl().keySet();
        for (String functionName : keySet) {
            Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName(functionName).stream()
                    .filter(m -> m.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE_FROM_JAR))
                    .forEach(m -> {
                        if (GlobalVariables.jdiContainer.containsKey(serviceId)) {
                            try {
                                List<Location> locationList = m.allLineLocations();

                                // filter based on source code & stageList
                                Map<String, Map<Integer, String>> sourceMap = DebuggerUtils.getSourceMap(serviceId);
                                // System.out.println("decompile:: " + sourceMap);
                                int findPercentage = 0;     // 1. find logger.debug, 2. String.format, 3. serviceId, functionId
                                String sourceLineCode = "";
                                for (Location loc : locationList) {
                                    // check, if source code is exist
                                    if(!sourceMap.containsKey(loc.sourcePath())) {
                                        findPercentage = 0; // reset
                                        continue;
                                    } else {
                                        sourceLineCode = sourceMap.get(loc.sourcePath()).get(loc.lineNumber());
                                        if(sourceLineCode == null || "".equals(sourceLineCode)) {
                                            findPercentage = 0; // reset
                                            continue;
                                        }
                                    }

                                    // analyze requirement filter
                                    if(sourceLineCode.indexOf("logger.debug(") >= 0) findPercentage++;
                                    else if(findPercentage == 1 && this.filterKey(serviceId, functionId, sourceLineCode) != null) findPercentage++;
                                    else findPercentage = 0;

                                    // check if, 2 requirement filter is fulfilled. so create breakpoint
                                    if(findPercentage < 2) continue;
                                    findPercentage = 0;

                                    // create breakpoints
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

                                    GlobalVariables.jdiContainer.get(serviceId).addBreakpointEvents(this.filterKey(serviceId, functionId, sourceLineCode), chainingBreakpointRequest);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

            j.vm().allClasses().forEach(c -> setConstructBrks.accept(c));
            j.onClassPrep(cp -> setConstructBrks.accept(cp.referenceType()));
        }
    }

    private String filterKey(String serviceId, String functionId, String sourceLineCode) {
        List<Map> brCollections = GlobalVariables.jdiContainer.get(serviceId).getBrColl(functionId);
        String keyFilter = null;
        for(Map map : brCollections) {
            if(sourceLineCode.indexOf(String.format("\"%s\", \"%s\", Long.valueOf(System.currentTimeMillis()) }));", functionId, map.get("name"))) >= 0) {
                keyFilter = String.format("%s#%s", functionId, map.get("name"));
                break;
            }
        }
        return keyFilter;
    }
}
