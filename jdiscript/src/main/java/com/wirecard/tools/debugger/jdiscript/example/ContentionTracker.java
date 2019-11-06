package com.wirecard.tools.debugger.jdiscript.example;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMonitorContendedEnter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.println;

public class ContentionTracker {
    int counter = 0;
    Set locations = new HashSet();
    Set sourceNames = new HashSet();
    Map<String, Integer> threads = new HashMap<>();

    public static void main(String args[]) {
        VirtualMachine vm = TomcatStarter.vm();
        JDIScript j = new JDIScript(vm);

        Map<Long, ContentionTracker> contended = new HashMap();
        OnMonitorContendedEnter monitorContendedEnter = it -> {
            try {
                ThreadReference tref = it.thread();
                ObjectReference mref = it.monitor();

                ContentionTracker t = contended.getOrDefault(mref.uniqueID(), new ContentionTracker());
                t.counter += 1;
                t.locations.add(it.location());
                t.sourceNames.addAll(mref.referenceType().sourcePaths(null));

                String threadKey = tref.name() + tref.uniqueID();
                int threadCount = t.threads.getOrDefault(threadKey, 0);
                t.threads.put(threadKey, threadCount + 1);
            } catch (AbsentInformationException e) {
                e.printStackTrace();
            }
        };

        j.monitorContendedEnterRequest().addHandler(monitorContendedEnter).enable();
        j.run(10 * 1000);

        println("Shutting down");
        vm.process().destroy();

        println("Contention info:");
        contended.forEach((k, v) -> {
            println("MonitorID: ${k}, Hits: ${v.counter}, Locations: " + v.locations);
            println("\tMonitor source: " + v.sourceNames);
            println("\tThreads: " + v.threads);
        });
    }

}