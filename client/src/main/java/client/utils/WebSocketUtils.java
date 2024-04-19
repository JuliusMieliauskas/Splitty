package client.utils;

import client.MyStompFrameHandler;
import client.scenes.EventOverviewCtrl;
import commons.EventUpdate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class WebSocketUtils {
    private StompSession session;
    private StompSession.Subscription subscription;
    private EventOverviewCtrl eventOverviewCtrl;

    public WebSocketUtils(String url, EventOverviewCtrl eventOverviewCtrl) {
        session = openSocket(url);
        this.eventOverviewCtrl = eventOverviewCtrl;
    }

    private StompSession openSocket(String url) {
        // strip https:// or http:// and ending /
        url = (url.startsWith("https://") ? url.substring(8) : (url.startsWith("http://") ? url.substring(7) : url));
        url = (url.endsWith("/") ? url.substring(0, url.length() - 1) : url);

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            return stompClient.connectAsync("ws://" + url + "/ws", new StompSessionHandlerAdapter() {
            }).get(3, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Subscribes to an url to listen for updates
     * @param eventId The event to listen for updates from
     */
    public void subscribe(Long eventId) {
        unsubscribe();
        subscription = session.subscribe("/api/events/websocket/" + eventId, new MyStompFrameHandler(this));
    }

    /**
     * Unsubscribes from the current subscription
     */
    public void unsubscribe() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    public void update(EventUpdate update) {
        eventOverviewCtrl.handleEventUpdate(update);
    }

    private List<Transport> createTransportClient() {
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return transports;
    }
}
