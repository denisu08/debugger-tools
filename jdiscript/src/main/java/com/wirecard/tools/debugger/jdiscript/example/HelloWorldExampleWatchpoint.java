package com.wirecard.tools.debugger.jdiscript.example;

import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.handlers.OnAccessWatchpoint;
import com.wirecard.tools.debugger.jdiscript.handlers.OnStep;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.StepEvent;

public class HelloWorldExampleWatchpoint {
    public static void main(final String[] args) {
        String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
        String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);

        VirtualMachine vm = null;
        try {
            vm = new VMLauncher(OPTIONS, MAIN).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final JDIScript j = new JDIScript(vm);

        final StringReference alt = j.vm().mirrorOf("JDIScript!");

        j.onFieldAccess(MAIN, "helloTo",
                new OnAccessWatchpoint() {
                    public void accessWatchpoint(AccessWatchpointEvent e) {
                        final ObjectReference obj = e.object();
                        final Field field = e.field();
                        j.onStepInto(e.thread(), j.once(new OnStep() {
                            public void step(StepEvent e) {
                                try {
                                    obj.setValue(field, alt);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }));
                    }
                });

        j.run();
    }

}
