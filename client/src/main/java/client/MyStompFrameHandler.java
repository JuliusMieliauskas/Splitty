package client;

import client.utils.WebSocketUtils;
import commons.EventUpdate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;

public class MyStompFrameHandler implements StompFrameHandler {
    private final WebSocketUtils util;
    public MyStompFrameHandler(WebSocketUtils util) {
        this.util = util;
    }
    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
        return EventUpdate.class;
    }

    @Override
    public void handleFrame(StompHeaders stompHeaders, Object o) {
        util.update((EventUpdate) o);
    }
}
