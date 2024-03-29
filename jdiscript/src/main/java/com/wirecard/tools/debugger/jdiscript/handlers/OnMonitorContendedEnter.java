package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.MonitorContendedEnterEvent;

@FunctionalInterface
public interface OnMonitorContendedEnter extends DebugLocatableHandler
{
    void monitorContendedEnter(MonitorContendedEnterEvent event);
}
