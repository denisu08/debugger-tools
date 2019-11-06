package com.wirecard.tools.debugger.controller;

import com.google.gson.Gson;
import org.json.simple.JSONArray;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class ServiceController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    private static final Gson gson = new Gson();

    @RequestMapping("/${spring.data.rest.base-path}")
    public String index() {
        return "not allowed";
    }

    @GetMapping(value = "/${spring.data.rest.base-path}/service/{serviceId}")
    public List queryServiceById(@DestinationVariable String serviceId) {
        JSONArray result = gson.fromJson("[{\"line\":1,\"name\":\"setValueUserId\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":2,\"name\":\"setValueUserName\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":3,\"name\":\"setCurrentPage\",\"stage\":\"Operation\",\"isDebug\":false}]", JSONArray.class);
        return result;
    }
}
