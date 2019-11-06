package com.wirecard.tools.debugger.jdiscript.example;

import com.sun.jdi.VirtualMachine;
import com.wirecard.tools.debugger.jdiscript.util.VMLauncher;

public class TomcatStarter {
    static final String TOMCAT = "/Users/jason/java_shite/apache-tomcat-5.5.27-eclipse";
    static final String OPTIONS =
            "-Dcatalina.home=" + TOMCAT +
                    " -Djava.endorsed.dirs=" + TOMCAT + "/common/endorsed" +
                    " -Dcatalina.base=" + TOMCAT +
                    " -Djava.io.tmpdir=" + TOMCAT + "}/temp" +
                    " -Dlog4j.configuration=file:/Users/Jason/java_shite/log4j.properties" +
                    " -cp ${TOMCAT}/bin/bootstrap.jar";
    static final String MAIN = "org.apache.catalina.startup.Bootstrap";

    static VirtualMachine vm() {
        return new VMLauncher(OPTIONS, MAIN).start();
    }

}
