package com.wirecard.tools.debugger.jdiscript.example;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.println;

import com.wirecard.tools.debugger.jdiscript.handlers.OnVMStart;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.util.VMSocketAttacher;

import com.sun.jdi.VirtualMachine;

class ContentionPrinter {   
    public static void main(String[] args) {
        VirtualMachine vm = new VMSocketAttacher(12345).attach();
        JDIScript j = new JDIScript(vm);

        j.monitorContendedEnterRequest(e -> {
            j.printTrace(e, "ContendedEnter for "+e.monitor());
        }).enable();

        j.monitorContendedEnteredRequest(e -> {
            long timestamp = System.currentTimeMillis();
            println(timestamp+": "+e.thread()+": ContendedEntered for "+e.monitor());
        }).enable();
        
        j.run((OnVMStart) e -> { println("Got StartEvent"); });

        println("Shutting down");
    }
}


