package com.wirecard.tools.exercise;

import com.wirecard.tools.exercise.HelloController;
import com.wirecard.tools.exercise.HelloWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorld.class);

    @RequestMapping({"/"})
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping({"/startMe"})
    public String startMe() {
        String dummy = "WowKeren";
        HelloWorld hello = new HelloWorld();
        DebuggerUtils.addDebuggerFlag("startMe#sayHello");
        logger.info(hello.sayHello());
        logger.info(hello.sayHello());
        DebuggerUtils.addDebuggerFlag("startMe#updateVar");
        hello.setHelloTo("Barney");
        logger.info(hello.sayHello());
        logger.info(hello.sayHello());
        DebuggerUtils.addDebuggerFlag("startMe#newInstance");
        hello = new HelloWorld("Fred", "Wilma");
        logger.info(hello.sayHello());
        logger.info(hello.sayHello());
        return "startMe";
    }

    @RequestMapping({"/searchUserByCriteria"})
    public String searchUserByCriteria() {
        DebuggerUtils.addDebuggerFlag("searchUserByCriteria#setValueUserId");
        logger.info("run:: searchUserByCriteria: setValueUserId");
        DebuggerUtils.addDebuggerFlag("searchUserByCriteria#setValueUserName");
        logger.info("run:: searchUserByCriteria: setValueUserName1");
        logger.info("run:: searchUserByCriteria: setValueUserName2");
        logger.info("run:: searchUserByCriteria: setValueUserName3");
        DebuggerUtils.addDebuggerFlag("searchUserByCriteria#getPCCUser");
        logger.info("run:: searchUserByCriteria: getPCCUser1");
        logger.info("run:: searchUserByCriteria: getPCCUser2");
        return "searchUserByCriteria";
    }

}
