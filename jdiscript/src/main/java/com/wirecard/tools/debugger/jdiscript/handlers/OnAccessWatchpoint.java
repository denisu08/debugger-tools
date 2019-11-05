package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.AccessWatchpointEvent;

@FunctionalInterface
public interface OnAccessWatchpoint extends DebugWatchpointHandler
{
    void accessWatchpoint(AccessWatchpointEvent event);
}
