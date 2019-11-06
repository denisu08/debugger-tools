package com.wirecard.tools.debugger.jdiscript.example;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.StepRequest;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;

public class HelloWorldExampleStepRequest {
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

        j.onFieldAccess(MAIN, "helloTo", it -> {
            final ObjectReference obj = it.object();
            final Field field = it.field();
            j.stepRequest(it.thread(),
                    StepRequest.STEP_MIN,
                    StepRequest.STEP_OVER, e -> {
                        try {
                            StringReference alttobe = j.vm().mirrorOf("Groovy");
                            obj.setValue(field, alttobe);
                            it.request().disable();
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                    }).enable();
        });

        j.run();

    }

}
