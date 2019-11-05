package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.ModificationWatchpointEvent;

@FunctionalInterface
public interface OnModificationWatchpoint extends DebugWatchpointHandler
{
    void modificationWatchpoint(ModificationWatchpointEvent event);
}
