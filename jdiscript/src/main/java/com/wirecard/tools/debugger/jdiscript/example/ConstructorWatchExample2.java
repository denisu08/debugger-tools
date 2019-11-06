package com.wirecard.tools.debugger.jdiscript.example;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.wirecard.tools.debugger.jdiscript.JDIScript;
import com.wirecard.tools.debugger.jdiscript.handlers.OnBreakpoint;
import com.wirecard.tools.debugger.jdiscript.handlers.OnMethodExit;
import com.wirecard.tools.debugger.jdiscript.handlers.OnVMStart;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;

import java.util.Stack;
import java.util.function.Consumer;

import static com.wirecard.tools.debugger.jdiscript.util.Utils.*;

class ConstructorWatchExample2 {

    String OPTIONS = ExampleConstant.CLASSPATH_CLASSES;
    String MAIN = String.format("%s.HelloWorld", ExampleConstant.PREFIX_PACKAGE);

    JDIScript j = new JDIScript(new VMLauncher(OPTIONS, MAIN).start());

    Stack<Method> stack = new Stack<>();

    //This references itself and so the compiler complains "Cannot
    //reference a field before it is defined" if we try to initialize directly.
    OnBreakpoint breakpoint;
    OnMethodExit methodExit = it -> {
        Method targetMethod = stack.peek();
        Method eventMethod = it.method();
        if (targetMethod.equals(eventMethod)) {
            j.deleteEventRequest(it.request());
            stack.pop();
            if (stack.isEmpty()) {
                j.breakpointRequests(breakpoint).forEach(e -> {
                    if (!e.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE)) {
                        e.disable();
                    }
                });
            }
        }
    };

    {
        breakpoint = be -> {
            String prefix = "  " + stack.size();
            ReferenceType refType = be.location().declaringType();
            println(prefix + "new " + refType.name());

            stack.push(be.location().method());

            try {
                j.breakpointRequests(breakpoint).forEach(it -> it.enable());
                j.methodExitRequest(methodExit)
                        .addInstanceFilter( be.thread().frame(0).thisObject() )
                        .enable();
            } catch (IncompatibleThreadStateException e) {
                e.printStackTrace();
            }

        };
    }

    OnVMStart start = se -> {
        /*Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName("<init>").stream()
                .filter(m -> !m.location().declaringType().name().equals("java.lang.Object"))
                .forEach(m -> j.breakpointRequest(m.location(), breakpoint).setEnabled(rt.name().startsWith(ExampleConstant.BASE_PACKAGE)));*/

        Consumer<ReferenceType> setConstructBrks = rt -> rt.methodsByName("<init>").stream()
                .filter(m -> m.location().declaringType().name().startsWith(ExampleConstant.BASE_PACKAGE))
                .forEach(m -> j.breakpointRequest(m.location(), breakpoint).setEnabled(true));

        j.vm().allClasses().forEach(c -> setConstructBrks.accept(c));
        j.onClassPrep(cp -> setConstructBrks.accept(cp.referenceType()));
    };

    public static void main(String[] args) {
        ConstructorWatchExample2 c = new ConstructorWatchExample2();
        c.j.run(c.start);
    }
}

