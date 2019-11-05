package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.MethodExitEvent;

@FunctionalInterface
public interface OnMethodExit extends DebugLocatableHandler
{
    void methodExit(MethodExitEvent event);
}
