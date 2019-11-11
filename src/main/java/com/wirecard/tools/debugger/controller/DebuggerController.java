package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.wirecard.tools.debugger.common.DebuggerUtils;
import com.wirecard.tools.debugger.common.GlobalVariables;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.example.ExampleConstant;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;
import com.wirecard.tools.debugger.jdiscript.util.VMSocketAttacher;
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

import java.util.*;
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
    public void attach(@DestinationVariable String serviceId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) {

        logger.info(debugMessage.getType() + ": " + debugMessage);

        try {
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
                    GlobalVariables.jdiContainer.get(serviceId).setIp(dataDebugFromClient.getIp());
                    GlobalVariables.jdiContainer.get(serviceId).setPort(dataDebugFromClient.getPort());
                    VirtualMachine vm = new VMSocketAttacher(GlobalVariables.jdiContainer.get(serviceId).getIp(), GlobalVariables.jdiContainer.get(serviceId).getPort()).attach();
                    GlobalVariables.jdiContainer.get(serviceId).setJdiScript(new JDIScript(vm));
                    GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vmDeathRequest(event -> {
                        this.detachConnection(serviceId);
                    });

                    this.collectBreakpointEvents(serviceId, debugMessage.getFunctionId());

                    GlobalVariables.jdiContainer.get(serviceId).setConnect(true);
                    messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
                    GlobalVariables.jdiContainer.get(serviceId).getJdiScript().run();
                    runCommand = false;
                    break;
                case DISCONNECT:
                    // remove & clean dataDebug from specific key
                    this.detachConnection(serviceId);
                    runCommand = false;
                    break;
                case NEXT:
                    String nextFilterKey = this.getNextFilterKey(GlobalVariables.jdiContainer.get(serviceId).getCpb());
                    ChainingBreakpointRequest breakpointRequest = GlobalVariables.jdiContainer.get(serviceId).getBreakpointEvents(debugMessage.getFunctionId()).get(nextFilterKey);
                    if (breakpointRequest != null) {
                        breakpointRequest.setEnabled(true);
                        GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vm().resume();

                        final Map selectedBreakpoint = this.getBreakpointByFilterkey(serviceId, nextFilterKey);
                        boolean isBreakpointEnabled = (Boolean) selectedBreakpoint.getOrDefault("isDebug", false) && !GlobalVariables.jdiContainer.get(serviceId).isMute();
                        breakpointRequest.setEnabled(isBreakpointEnabled);
                    } else {
                        GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vm().resume();
                    }
                    break;
                case RESUME:
                    GlobalVariables.jdiContainer.get(serviceId).getJdiScript().vm().resume();
                    break;
                case SET_BREAKPOINT:
                    GlobalVariables.jdiContainer.get(serviceId).putBrColl(debugMessage.getFunctionId(), dataDebugFromClient.getCurrentBrColl());
                    this.collectBreakpointEvents(serviceId, debugMessage.getFunctionId());
                    break;
                case MUTE:
                    GlobalVariables.jdiContainer.get(serviceId).setMute(dataDebugFromClient.isMute());
                    this.collectBreakpointEvents(serviceId, debugMessage.getFunctionId());
                    break;
                default:
                    break;
            }

            if (runCommand) {
                GlobalVariables.jdiContainer.get(serviceId).setCpb("xx");
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), "error#" + ex.getMessage());
        }
    }

    private void detachConnection(final String serviceId) {
        if (GlobalVariables.jdiContainer.containsKey(serviceId)) {
            try {
                GlobalVariables.jdiContainer.get(serviceId).clearAndDisconnect();
                GlobalVariables.jdiContainer.remove(serviceId);
                DebuggerUtils.removeSourceMap(serviceId);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(new DataDebug()));
            } catch (Exception e) {
                e.printStackTrace();
                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), "error#" + e.getMessage());
            }
        }
    }

    private void collectBreakpointEvents(final String serviceId, final String functionId) {
        DataDebug dataDebug = GlobalVariables.jdiContainer.get(serviceId);
        JDIScript j = dataDebug.getJdiScript();
        if (j == null) return;

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
                                    if (!sourceMap.containsKey(loc.sourcePath())) {
                                        findPercentage = 0; // reset
                                        continue;
                                    } else {
                                        sourceLineCode = sourceMap.get(loc.sourcePath()).get(loc.lineNumber());
                                        if (sourceLineCode == null || "".equals(sourceLineCode)) {
                                            findPercentage = 0; // reset
                                            continue;
                                        }
                                    }

                                    // analyze requirement filter
                                    if (sourceLineCode.indexOf("logger.debug(") >= 0) findPercentage++;
                                    else if (findPercentage == 1 && this.filterKey(serviceId, functionId, sourceLineCode) != null)
                                        findPercentage++;
                                    else findPercentage = 0;

                                    // check if, 2 requirement filter is fulfilled. so create breakpoint
                                    if (findPercentage < 2) continue;
                                    findPercentage = 0;

                                    // create breakpoints, if there are no breakpoint in globalVariables
                                    final Map selectedBreakpoint = this.filterKey(serviceId, functionId, sourceLineCode);
                                    String filterKey = String.format("%s#%s", functionId, selectedBreakpoint.get("line"));

                                    ChainingBreakpointRequest chainingBreakpointRequest = dataDebug.getBreakpointEvents(serviceId).get(filterKey);
                                    if (chainingBreakpointRequest == null) {
                                        chainingBreakpointRequest = j.breakpointRequest(loc, be -> {
                                            System.out.println("be: " + be);
                                            try {
                                                // start - grab system variables
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
                                                for (LocalVariable variable : localVariables) {
                                                    if (variable.isVisible(stackFrame)) {
                                                        Value val = stackFrame.getValue(variable);
                                                        sysVar.put(variable.name(), DebuggerUtils.getJavaValue(val, be.thread()));
                                                    }
                                                }
                                                GlobalVariables.jdiContainer.get(serviceId).setSysVar(sysVar);
                                                // end - grab system variables

                                                Map customVar = new HashMap<>();
                                                Value val = DebuggerUtils.evaluate(be.thread().frame(0), "\"testKeren\".length()");
                                                customVar.put("\"testKeren\".length()", DebuggerUtils.getJavaValue(val, be.thread()));
                                                GlobalVariables.jdiContainer.get(serviceId).setCustVar(customVar);

                                                GlobalVariables.jdiContainer.get(serviceId).setCpb(filterKey);
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), om.writeValueAsString(GlobalVariables.jdiContainer.get(serviceId)));
                                                j.vm().suspend();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), "error#" + ex.getMessage());
                                            }
                                        });
                                        GlobalVariables.jdiContainer.get(serviceId).addBreakpointEvents(functionId, filterKey, chainingBreakpointRequest);
                                    }

                                    boolean isBreakpointEnabled = (Boolean) selectedBreakpoint.getOrDefault("isDebug", false) && !GlobalVariables.jdiContainer.get(serviceId).isMute();
                                    GlobalVariables.jdiContainer.get(serviceId).getBreakpointEvents(functionId).get(filterKey).setEnabled(isBreakpointEnabled);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                messagingTemplate.convertAndSend(format("/debug-channel/%s", serviceId), "error#" + ex.getMessage());
                            }
                        }
                    });

            j.vm().allClasses().forEach(c -> setConstructBrks.accept(c));
            j.onClassPrep(cp -> setConstructBrks.accept(cp.referenceType()));
        }
    }

    private Map getBreakpointByFilterkey(String serviceId, String filterKey) {
        String[] filters = filterKey.split("#");
        List<Map> brCollections = GlobalVariables.jdiContainer.get(serviceId).getBrColl(filters[0]);
        Map breakpointSelected = null;
        for (Map map : brCollections) {
            if (filters[1].equalsIgnoreCase(String.valueOf(map.get("line")))) {
                breakpointSelected = map;
                break;
            }
        }
        return breakpointSelected;
    }

    private Map filterKey(String serviceId, String functionId, String sourceLineCode) {
        List<Map> brCollections = GlobalVariables.jdiContainer.get(serviceId).getBrColl(functionId);
        Map breakpointSelected = null;
        for (Map map : brCollections) {
            if (sourceLineCode.indexOf(String.format("\"%s\", \"%s\", Long.valueOf(System.currentTimeMillis()) }));", functionId, map.get("name"))) >= 0) {
                breakpointSelected = map;
                break;
            }
        }
        return breakpointSelected;
    }

    private String getNextFilterKey(String filterKey) {
        String[] splitKeys = filterKey.split("#");
        return String.format("%s#%s", splitKeys[0], Integer.parseInt(splitKeys[1]) + 1);
    }
}
