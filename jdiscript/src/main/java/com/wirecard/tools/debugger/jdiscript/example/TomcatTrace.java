package com.wirecard.tools.debugger.jdiscript.example;

import com.wirecard.tools.debugger.jdiscript.JDIScript;

import com.sun.jdi.VirtualMachine;

import static com.wirecard.tools.debugger.jdiscript.example.ConnectorInspector.println;

public class TomcatTrace {
    public static void main(String args[]) {
        VirtualMachine vm = TomcatStarter.vm();
        JDIScript jdi = new JDIScript(vm);
        jdi.run(new TraceExampleHandler(jdi), 10 * 1000);

        println("Shutting down");
        vm.process().destroy();
    }
}
