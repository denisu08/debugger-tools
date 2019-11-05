package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.Event;

@FunctionalInterface
public interface OnEvent extends DebugEventHandler
{
    void event(Event event);
}
