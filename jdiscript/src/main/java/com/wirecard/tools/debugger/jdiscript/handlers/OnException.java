package com.wirecard.tools.debugger.jdiscript.handlers;

import com.sun.jdi.event.ExceptionEvent;

@FunctionalInterface
public interface OnException extends DebugLocatableHandler
{
    void exception(ExceptionEvent e);
}
