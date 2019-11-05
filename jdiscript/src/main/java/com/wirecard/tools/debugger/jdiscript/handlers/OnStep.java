package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.StepEvent;

@FunctionalInterface
public interface OnStep extends DebugLocatableHandler
{
    void step(StepEvent event);
}
