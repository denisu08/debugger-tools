package com.wirecard.tools.debugger.model;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;

public class CurrentState {
    private Location currentLocation;
    private BreakpointEvent currentEvent;

    public CurrentState(Location currentLocation, BreakpointEvent currentEvent) {
        this.currentLocation = currentLocation;
        this.currentEvent = currentEvent;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public BreakpointEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(BreakpointEvent currentEvent) {
        this.currentEvent = currentEvent;
    }
}
