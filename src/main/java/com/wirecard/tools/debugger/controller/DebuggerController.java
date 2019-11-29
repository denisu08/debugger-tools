package com.wirecard.tools.debugger.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.wirecard.tools.debugger.common.DebuggerConstant;
import com.wirecard.tools.debugger.common.DebuggerUtils;
import com.wirecard.tools.debugger.common.GlobalVariables;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.requests.ChainingBreakpointRequest;
import com.wirecard.tools.debugger.jdiscript.util.VMSocketAttacher;
import com.wirecard.tools.debugger.model.CurrentState;
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
            String plainContent = new String(Base64.getDecoder().decode(debugMessage.getContent()));
            DataDebug dataDebugFromClient = this.om.readValue(plainContent, DataDebug.class);
            boolean runCommand = true;

            // if null, so client is pointed as latest data
            if (dataDebug == null) {
                dataDebug = dataDebugFromClient;
                GlobalVariables.jdiContainer.put(processFlowGeneratorId, dataDebug);
            }

            switch (debugMessage.getType()) {
                case SYNC:
                    if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
                        messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                    }
                    runCommand = false;
                    break;
                case CONNECT:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setIp(dataDebugFromClient.getIp());
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setPort(dataDebugFromClient.getPort());

                    if (GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript() == null) {
                        VirtualMachine vm = new VMSocketAttacher(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getIp(), GlobalVariables.jdiContainer.get(processFlowGeneratorId).getPort()).attach();
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).setJdiScript(new JDIScript(vm));
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vmDeathRequest(event -> {
                            this.detachConnection(processFlowGeneratorId);
                        });
                        this.collectBreakpointEvents(processFlowGeneratorId, debugMessage.getFunctionId());
                    }

                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setConnect(true);
                    messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().run();
                    runCommand = false;
                    break;
                case DISCONNECT:
                    // remove & clean dataDebug from specific key
                    this.detachConnection(processFlowGeneratorId);
                    runCommand = false;
                    break;
                case NEXT:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).putBrColl(debugMessage.getFunctionId(), dataDebugFromClient.getCurrentBrColl());
                    this.collectBreakpointEvents(processFlowGeneratorId, debugMessage.getFunctionId());

                    String nextFilterKey = this.getNextFilterKey(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getCpb());
                    ChainingBreakpointRequest breakpointRequest = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(debugMessage.getFunctionId()).get(nextFilterKey);
                    if (breakpointRequest != null) {
                        breakpointRequest.setEnabled(true);
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();
                    } else {
                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();
                    }
                    runCommand = false;
                    break;
                case RESUME:
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript().vm().resume();
                    break;
                case SET_VARIABLE: // variables
                    GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCustVar(dataDebugFromClient.getCustVar());
                    if (GlobalVariables.jdiContainer.get(processFlowGeneratorId).getCpb() != null && !DebuggerConstant.DEFAULT_POINTER_BREAKPOINT.equals(GlobalVariables.jdiContainer.get(processFlowGeneratorId).getCpb())) {
                        this.queryCustomVariables(processFlowGeneratorId, GlobalVariables.currentState.get(processFlowGeneratorId).getCurrentEvent());
                        messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
                        runCommand = false;
                    }
                    break;
                case SET_BREAKPOINT: // breakpoints
                    // logger.info("dataDebugFromClient: " + dataDebugFromClient.getCurrentBrColl());
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
                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCpb(DebuggerConstant.DEFAULT_POINTER_BREAKPOINT);
                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), DebuggerConstant.ERROR_PREFIX + ex.getMessage());
        }
    }

    private void detachConnection(final String processFlowGeneratorId) {
        if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
            try {
                GlobalVariables.jdiContainer.get(processFlowGeneratorId).disconnect();
                // no remove data & source code
                // GlobalVariables.jdiContainer.remove(processFlowGeneratorId);
                // DebuggerUtils.removeSourceMap(processFlowGeneratorId);
                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));
            } catch (Exception e) {
                e.printStackTrace();
                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), DebuggerConstant.ERROR_PREFIX + e.getMessage());
            }
        }
    }

    private void collectBreakpointEvents(final String processFlowGeneratorId, final String functionId) {
        JDIScript j = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getJdiScript();
        if (j == null) return;

        Set<String> keySet = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrColl().keySet();
        // logger.info("collectBreakpointEvents: " + keySet);
        for (String funcKey : keySet) {
            // logger.info("collect: " + funcKey);
            String[] functions = funcKey.split(DebuggerConstant.DEBUGGER_FORMAT_PARAM);
            String currentClassName = functions[0].trim();
            // String currentClassName = String.format("%sserviceimpl", tmpClassName);
            String currentMethodName = functions[1].trim();

            Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName(currentMethodName).stream()
                    .filter(m -> m.location() != null && m.location().declaringType() != null
                            && m.location().declaringType().name() != null && m.location().declaringType().name().toLowerCase().contains(currentClassName.toLowerCase()))
                    .forEach(m -> {
                        if (GlobalVariables.jdiContainer.containsKey(processFlowGeneratorId)) {
                            try {
                                List<Location> locationList = m.allLineLocations();

                                // filter based on source code & stageList
                                Map<String, Map<Integer, String>> sourceMap = DebuggerUtils.getSourceMap(processFlowGeneratorId);
                                // logger.info("source key: " + sourceMap.keySet());
                                // logger.info("sourceMap: " + sourceMap);
                                String sourceLineCode = DebuggerConstant.EMPTY_STRING;
                                for (Location loc : locationList) {
                                    String sourcePath = loc.sourcePath().replaceAll("\\\\", "/");
                                    // logger.info("source path(" + loc.lineNumber() + "): " + sourcePath);
                                    // check, if source code is exist
                                    if (!sourceMap.containsKey(sourcePath)) continue;
                                    sourceLineCode = sourceMap.get(sourcePath).get(loc.lineNumber());
                                    // logger.info("sourceLineCode(" + loc.lineNumber() + "): " + sourceLineCode);
                                    if (sourceLineCode == null || DebuggerConstant.EMPTY_STRING.equals(sourceLineCode))
                                        continue;

                                    // analyze requirement filter
                                    if (this.filterKeyBySourceLineCode(processFlowGeneratorId, functionId, sourceLineCode) == null)
                                        continue;

                                    // create breakpoints, if there are no breakpoint in globalVariables
                                    final Map selectedBreakpoint = this.filterKeyBySourceLineCode(processFlowGeneratorId, functionId, sourceLineCode);
                                    if (selectedBreakpoint == null) continue;
                                    String filterKey = String.format(DebuggerConstant.DEBUGGER_FORMAT_CODE, functionId, selectedBreakpoint.get(DebuggerConstant.KEY_DEBUG_LINE));

                                    ChainingBreakpointRequest chainingBreakpointRequest = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(functionId).get(filterKey);
                                    if (chainingBreakpointRequest == null) {
                                        // logger.info("create breakpoint: " + filterKey);
                                        chainingBreakpointRequest = j.breakpointRequest(loc, be -> {
                                            logger.info("be: " + be);
                                            try {
                                                // start - grab system variables
                                                // get field current class
                                                List<Field> childFields = m.location().declaringType().allFields();
                                                // StackFrame stackFrame = be.thread().frame(0);
                                                Map sysVar = new HashMap<>();
                                                for (Field childField : childFields) {
                                                    if (!childField.isStatic()) {
                                                        Value val = be.thread().frame(0).thisObject().getValue(childField);
                                                        sysVar.put(childField.name(), DebuggerUtils.getJavaValue(val, be.thread()));
                                                    }
                                                }

                                                // get field local variable
                                                List<LocalVariable> localVariables = loc.method().variables();
                                                for (LocalVariable variable : localVariables) {
                                                    if (variable.isVisible(be.thread().frame(0))) {
                                                        Value val = be.thread().frame(0).getValue(variable);
                                                        sysVar.put(variable.name(), DebuggerUtils.getJavaValue(val, be.thread()));
                                                    }
                                                }
                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setSysVar(sysVar);
                                                // end - grab system variables

                                                // query custom variables
                                                this.queryCustomVariables(processFlowGeneratorId, be);
                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCpb(filterKey);
                                                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), om.writeValueAsString(GlobalVariables.jdiContainer.get(processFlowGeneratorId)));

                                                // stop, to the next breakpoint
                                                j.vm().suspend();

                                                // rollback enable flag in breakpoint request to original flag
                                                boolean isBreakpointEnabled = (Boolean) selectedBreakpoint.getOrDefault(DebuggerConstant.KEY_DEBUG_FLAG, false) && !GlobalVariables.jdiContainer.get(processFlowGeneratorId).isMute();
                                                GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(functionId).get(filterKey).setEnabled(isBreakpointEnabled);

                                                // set current thread
                                                GlobalVariables.currentState.put(processFlowGeneratorId, new CurrentState(loc, be));
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), DebuggerConstant.ERROR_PREFIX + ex.getMessage());
                                            }
                                        });
                                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).addBreakpointEvents(functionId, filterKey, chainingBreakpointRequest);
                                    }

                                    // set enable flag for breakpoint request
                                    if (GlobalVariables.jdiContainer.get(processFlowGeneratorId).isMute()) {
                                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(functionId).get(filterKey).setEnabled(false);
                                    } else {
                                        GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBreakpointEvents(functionId).get(filterKey).setEnabled((Boolean) selectedBreakpoint.getOrDefault(DebuggerConstant.KEY_DEBUG_FLAG, false));
                                    }
                                }
                            } catch (AbsentInformationException aix) {
                                logger.error("*error: " + m.location(), aix);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                messagingTemplate.convertAndSend(format(DebuggerConstant.DEBUGGER_CHANNEL_FORMAT, processFlowGeneratorId), DebuggerConstant.ERROR_PREFIX + ex.getMessage());
                            }
                        }
                    });

            Set<String> listClasses = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrClasses();
            Set<String> classUnique = new HashSet<>();
            j.vm().allClasses().forEach(c -> {
                if (!c.name().contains("$$") && listClasses.stream().anyMatch(entry -> c.name().toLowerCase().contains(entry.toLowerCase())) && classUnique.add(c.name())) {
                    setConstructBrks.accept(c);
                }
            });
            j.onClassPrep(cp -> {
                if (!cp.referenceType().name().contains("$$") && listClasses.stream().anyMatch(entry -> cp.referenceType().name().toLowerCase().contains(entry.toLowerCase())) && classUnique.add(cp.referenceType().name())) {
                    setConstructBrks.accept(cp.referenceType());
                }
            });
        }
    }

    private void queryCustomVariables(final String processFlowGeneratorId, BreakpointEvent be) {
        // example: "testKeren".length()
        Map<String, String> customVariables = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getCustVar();
        for (String key : customVariables.keySet()) {
            try {
                Value val = DebuggerUtils.evaluate(be.thread().frame(0), key);
                customVariables.put(key, String.valueOf(DebuggerUtils.getJavaValue(val, be.thread())));
            } catch (Exception ex) {
                customVariables.put(key, "*error: " + ex.getMessage());
            }
        }
        GlobalVariables.jdiContainer.get(processFlowGeneratorId).setCustVar(customVariables);
    }

    private Map getBreakpointByFilterkey(String processFlowGeneratorId, String filterKey) {
        String[] filters = filterKey.split(DebuggerConstant.DEBUGGER_SEPARATOR_CODE);
        List<Map> brCollections = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrColl(filters[0]);
        Map breakpointSelected = null;
        for (Map map : brCollections) {
            if (filters[1].equalsIgnoreCase(String.valueOf(map.get(DebuggerConstant.KEY_DEBUG_LINE)))) {
                breakpointSelected = map;
                break;
            }
        }
        return breakpointSelected;
    }

    private Map filterKeyBySourceLineCode(String processFlowGeneratorId, String fParam, String sourceLineCode) {
        List<Map> brCollections = GlobalVariables.jdiContainer.get(processFlowGeneratorId).getBrColl(fParam);
        Map breakpointSelected = null;
        String[] functions = fParam.split(DebuggerConstant.DEBUGGER_FORMAT_PARAM);
        String functionId = functions[1].trim();
        if(brCollections != null) {
            for (Map map : brCollections) {
                if (sourceLineCode.indexOf(String.format("DebuggerUtils.addDebuggerFlag(\"%s#%s\")", functionId, map.get(DebuggerConstant.KEY_DEBUG_NAME))) >= 0) {
                    breakpointSelected = map;
                    break;
                }
            }
        }
        return breakpointSelected;
    }

    private String getNextFilterKey(String filterKey) {
        String[] splitKeys = filterKey.split(DebuggerConstant.DEBUGGER_SEPARATOR_CODE);
        return String.format(DebuggerConstant.DEBUGGER_FORMAT_CODE, splitKeys[0], Integer.parseInt(splitKeys[1]) + 1);
    }
}
