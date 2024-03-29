package com.wirecard.tools.debugger.config;

import com.wirecard.tools.debugger.common.DebuggerConstant;
import com.wirecard.tools.debugger.common.DebuggerUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.MultiValueMap;

public class DebuggerChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        MessageHeaders headers = message.getHeaders();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // compile source when connect is triggered
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            MultiValueMap<String, String> multiValueMap = headers.get(StompHeaderAccessor.NATIVE_HEADERS, MultiValueMap.class);
            if (multiValueMap.containsKey(DebuggerConstant.DEBUGGER_CHANNEL_ID)) {
                try {
                    DebuggerUtils.getSourceMap(multiValueMap.getFirst(DebuggerConstant.DEBUGGER_CHANNEL_ID), multiValueMap.getFirst(DebuggerConstant.DEBUGGER_JAR_PATH));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return message;
    }
}
