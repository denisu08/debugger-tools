package com.wirecard.tools.debugger.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class DebuggerUtilsTest {
    @Test
    public void testDecompile() throws Exception {
        Map<String, Map<Integer, String>> sourceMap = DebuggerUtils.getSourceMap("12345", "testJar/ProcessFlow_PCCCUUserManagement-service-1.0.jar");
        System.out.println("sourceMap: " + sourceMap);
    }
}
