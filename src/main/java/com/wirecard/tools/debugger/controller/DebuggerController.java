package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.wirecard.tools.debugger.common.DebuggerConstant;
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

    @MessageMapping("/debugger/{processFlowGeneratorId}")
    public void attach(@DestinationVariable String processFlowGeneratorId, @Payload DebugMessage debugMessage, SimpMessageHeaderAccessor headerAccessor) {

        logger.info(debugMessage.getType() + ": " + debugMessage);

        try {
            DataDebug dataDebug = GlobalVariables.jdiContainer.get(processFlowGeneratorId);

            if (DebugMessage.CommandType.SYNC == debugMessage.getType()) {
                if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
                    messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                }
                return;
            }

            String plainContent = new String(Base64.getDecoder().decode(debugMessage.getContent()));
            DataDebug dataDebugFromClient = this.om.readValue(plainContent, DataDebug.class);
            boolean runCommand = true;

            // if null, so client is pointed as latest data
            if (dataDebug == null) {
                dataDebug = dataDebugFromClient;
                GlobalVariables.jdiContainer.put(processFlowGeneratorId, dataDebug);
            }

            switch (debugMessage.getType()) {
                case CONNECT:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setIp(dataDebugFromClient.getIp());
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setPort(dataDebugFromClient.getPort());

                    // TODO: checking (ip + port + processFlowGeneratorId) for connect to runtime server

                    if (GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript() == null) {
                        VirtualMachine vm = new VMSocketAttacher(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getIp(), GlobalVariables.jdiContainer.get(processFlowGeneratorId).getPort()).attach();
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).setJdiScript(new JDIScript(vm));
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vmDeathRequest(event -> {
                            this.detachConnection(processFlowGeneratorId);
                        });
                        this.collectBreakpointEvents(processFlowGeneratorId, debugMessage.getFunctionId());
                    }

                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setConnect(true);
                    messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().run();
                    runCommand = false;
                    break;
                case DISCONNECT:
                    // remove & clean dataDebug from specific key
                    this.detachConnection(processFlowGeneratorId);
                    runCommand = false;
                    break;
                case NEXT:
                    String nextFilterKey = this.getNextFilterKey(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getCpb());
                    ChainingBreakpointRequest breakpointRequest = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(debugMessage.getFunctionId()).get(nextFilterKey);
                    if (breakpointRequest != null) {
                        breakpointRequest.setEnabled(true);
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();

                        final Map selectedBreakpoint = this.getBreakpointByFilterkey(processFlowGeneratorId, nextFilterKey);
                        boolean isBreakpointEnabled = (Boolean) selectedBreakpoint.getOrDefault("isDebug", false) && !GlobalVariables.jdiContainer.get(processFlowGeneratorId).isMute();
                        breakpointRequest.setEnabled(isBreakpointEnabled);
                    } else {
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();
                    }
                    break;
                case RESUME:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();
                    break;
                case SET_BREAKPOINT:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).putBrColl(debugMessage.getFunctionId(), dataDebugFromClient.getCurrentBrColl());
                    this.collectBreakpointEvents(processFlowGeneratorId, debugMessage.getFunctionId());
                    break;
                case MUTE:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setMute(dataDebugFromClient.isMute());
                    this.collectBreakpointEvents(processFlowGeneratorId, debugMessage.getFunctionId());
                    break;
                default:
                    break;
            }

            if (runCommand) {
                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCpb("xx");
                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), "error#" + ex.getMessage());
        }
    }

    private void detachConnection(final String processFlowGeneratorId) {
        if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
            try {
                GlobalVariables.jdiContainer.get(processFlowGeneratorId).clearAndDisconnect();
                GlobalVariables.jdiContainer.remove(processFlowGeneratorId);
                DebuggerUtils.removeSourceMap(processFlowGeneratorId);
                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), om.writeValueAsString(new DataDebug()));
            } catch (Exception e) {
                e.printStackTrace();
                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), "error#" + e.getMessage());
            }
        }
    }

    private void collectBreakpointEvents(final String processFlowGeneratorId, final String functionId) {
        DataDebug dataDebug = GlobalVariables.jdiContainer.get(processFlowGeneratorId);
        JDIScript j = dataDebug.getJdiScript();
        if (j == null) return;

        Set<String> keySet = dataDebug.getBrColl().keySet();
        for (String functionName : keySet) {
            Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName(functionName).stream()
                    .filter(m -> m.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE_FROM_JAR))
                    .forEach(m -> {
                        if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
                            try {
                                List<Location> locationList = m.allLineLocations();

                                // filter based on source code & stageList
                                Map<String, Map<Integer, String>> sourceMap = DebuggerUtils.getSourceMap(processFlowGeneratorId);
                                String sourceLineCode = "";
                                for (Location loc : locationList) {
                                    // check, if source code is exist
                                    if (!sourceMap.containsKey(loc.sourcePath())) continue;
                                    sourceLineCode = sourceMap.get(loc.sourcePath()).get(loc.lineNumber());
                                    if (sourceLineCode == null || "".equals(sourceLineCode)) continue;

                                    // analyze requirement filter
                                    if (this.filterKeyBySourceLineCode(processFlowGeneratorId, functionId, sourceLineCode) == null)
                                        continue;

                                    // create breakpoints, if there are no breakpoint in globalVariables
                                    final Map selectedBreakpoint = this.filterKeyBySourceLineCode(processFlowGeneratorId, functionId, sourceLineCode);
                                    String filterKey = String.format("%s#%s", functionId, selectedBreakpoint.get("line"));

                                    ChainingBreakpointRequest chainingBreakpointRequest = dataDebug.getBreakpointEvents(processFlowGeneratorId).get(filterKey);
                                    if (chainingBreakpointRequest == null) {
                                        chainingBreakpointRequest = j.breakpointRequest(loc, be -> {
                                            logger.info("be: " + be);
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
                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setSysVar(sysVar);
                                                // end - grab system variables

                                                Map customVar = new HashMap<>();
                                                Value val = DebuggerUtils.evaluate(be.thread().frame(0), "\"testKeren\".length()");
                                                customVar.put("\"testKeren\".length()", DebuggerUtils.getJavaValue(val, be.thread()));
                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCustVar(customVar);

                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCpb(filterKey);
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                                                j.vm().suspend();
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), "error#" + ex.getMessage());
                                            }
                                        });
                                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).addBreakpointEvents(functionId, filterKey, chainingBreakpointRequest);
                                    }

                                    boolean isBreakpointEnabled = (Boolean) selectedBreakpoint.getOrDefault("isDebug", false) && !GlobalVariables.jdiContainer.get(processFlowGeneratorId).isMute();
                                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(functionId).get(filterKey).setEnabled(isBreakpointEnabled);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                messagingTemplate.convertAndSend(format("/debug-channel/%s", processFlowGeneratorId), "error#" + ex.getMessage());
                            }
                        }
                    });

            j.vm().allClasses().forEach(c -> setConstructBrks.accept(c));
            j.onClassPrep(cp -> setConstructBrks.accept(cp.referenceType()));
        }
    }

    private Map getBreakpointByFilterkey(String processFlowGeneratorId, String filterKey) {
        String[] filters = filterKey.split("#");
        List<Map> brCollections = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrColl(filters[0]);
        Map breakpointSelected = null;
        for (Map map : brCollections) {
            if (filters[1].equalsIgnoreCase(String.valueOf(map.get("line")))) {
                breakpointSelected = map;
                break;
            }
        }
        return breakpointSelected;
    }

    private Map filterKeyBySourceLineCode(String processFlowGeneratorId, String functionId, String sourceLineCode) {
        List<Map> brCollections = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrColl(functionId);
        Map breakpointSelected = null;
        for (Map map : brCollections) {
            if (sourceLineCode.indexOf(String.format("DebuggerUtils.addDebuggerFlag(\"%s#%s\")", functionId, map.get("name"))) >= 0) {
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
