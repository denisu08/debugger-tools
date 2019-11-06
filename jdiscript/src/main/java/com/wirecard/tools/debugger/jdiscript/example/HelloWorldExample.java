package com.wirecard.tools.debugger.jdiscript.example;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.unchecked;

import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;

public class HelloWorldExample {

    public static void main(final String[] args) {

        String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
        String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);

        JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());

        j.onFieldAccess(MAIN, "helloTo", e -> {
            j.onStepInto(e.thread(), j.once(se -> {
                unchecked(() -> e.object().setValue(e.field(),
                        j.vm().mirrorOf("JDIScript!")));
            }));
        });

        j.run();
    }
}
