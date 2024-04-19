package server.api;

import commons.Event;
import commons.EventUpdate;
import commons.Expense;
import commons.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketTest {
    @Autowired
    private TestRestTemplate restTemplate;
    static private HttpHeaders headers;
    @LocalServerPort
    private int port;
    private String URL;
    private CompletableFuture<EventUpdate> completableFuture;

    @BeforeAll
    static void createHeaders() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @BeforeEach
    void setup() {
        URL = "ws://localhost:" + port + "/ws";
        completableFuture = new CompletableFuture<>();
    }

    private final class EventUpdateStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return EventUpdate.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            completableFuture.complete((EventUpdate) o);
        }
    }

    private List<Transport> createTransportClient() {
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return transports;
    }

    private StompSession getSession() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            return stompClient.connectAsync(URL, new StompSessionHandlerAdapter() {
            }).get(3, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * POST /api/events/{id}/users
     * Assumes GET /api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void addUserUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        User user = restTemplate.getForEntity("/api/users/6", User.class).getBody();
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/events/1/users", request, String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), response.getBody());


        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(update);
        assertEquals(EventUpdate.Action.ADDED_USER, update.getAction());
        assertEquals(6, update.getObjectId());
    }

    /**
     * DELETE /api/events/{eid}/users/{id}
     */
    @Test
    @DirtiesContext
    void removeUserUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/events/1/users/2");

        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(update);
        assertEquals(EventUpdate.Action.REMOVED_USER, update.getAction());
        assertEquals(2, update.getObjectId());
    }

    /**
     * DELETE /api/users/{id}
     * Assumes DELETE api/expenses is working
     */
    @Test
    @DirtiesContext
    void deleteUserUpdate() {
        restTemplate.delete("/api/expenses");

        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/users/2");

        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(update);
        assertEquals(EventUpdate.Action.REMOVED_USER, update.getAction());
        assertEquals(2, update.getObjectId());
    }

    /**
     * PUT api/users/{id}
     * Assumes GET /api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void updateUserUpdate() {
        restTemplate.delete("/api/expenses");

        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        User modifiedUser = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        modifiedUser.setUsername("New Walter");
        HttpEntity<User> request = new HttpEntity<>(modifiedUser, headers);
        restTemplate.put("/api/users/1", request, User.class);

        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(update);
        assertEquals(EventUpdate.Action.UPDATED_USER, update.getAction());
        assertEquals(1, update.getObjectId());
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void addExpenseUpdate() {
        restTemplate.delete("/api/expenses");

        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Event event = restTemplate.getForEntity("/api/events/1", Event.class).getBody();
        Expense expense = new Expense(10., "Test expense", user, event);
        HttpEntity<Expense> request = new HttpEntity<>(expense, headers);
        Expense posted = restTemplate.postForEntity("/api/expenses", request, Expense.class).getBody();

        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(EventUpdate.Action.ADDED_EXPENSE, update.getAction());
        assertEquals(posted.getId(), update.getObjectId());
    }

    /**
     * DELETE api/expenses/{id}
     */
    @Test
    @DirtiesContext
    void removeExpenseUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/expenses/1");

        EventUpdate update = null;
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(update);
        assertEquals(EventUpdate.Action.REMOVED_EXPENSE, update.getAction());
        assertEquals(1, update.getObjectId());
    }

    /**
     * DELETE api/expenses
     */
    @Test
    @DirtiesContext
    void removeAllExpensesUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/expenses");

        EventUpdate update = null;
        completableFuture = new CompletableFuture<>();
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertNotNull(update);
        assertEquals(EventUpdate.Action.REMOVED_ALL_EXPENSES, update.getAction());
    }

    /**
     * PUT api/expenses/{id}
     * Assumes GET /api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void updateExpenseUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        ResponseEntity<Expense> responseGet = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        Expense modifiedExpense = responseGet.getBody();
        modifiedExpense.setTitle("New Eggs");
        HttpEntity<Expense> request = new HttpEntity<>(modifiedExpense, headers);
        restTemplate.put("/api/expenses/1", request, Expense.class);

        EventUpdate update = null;
        completableFuture = new CompletableFuture<>();
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertNotNull(update);
        assertEquals(EventUpdate.Action.UPDATED_EXPENSE, update.getAction());
        assertEquals(1, update.getObjectId());
    }

    /**
     * PUT api/events/{id}
     * Assumes GET /api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void updateEventUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        ResponseEntity<Event> responseGet = restTemplate.getForEntity("/api/events/1", Event.class);
        Event modifiedEvent = responseGet.getBody();
        modifiedEvent.setTitle("New title");
        HttpEntity<Event> request = new HttpEntity<>(modifiedEvent, headers);
        restTemplate.put("/api/events/1", request, Event.class);

        EventUpdate update = null;
        completableFuture = new CompletableFuture<>();
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertNotNull(update);
        assertEquals(EventUpdate.Action.UPDATED_EVENT, update.getAction());
    }

    /**
     * DELETE api/events/{id}
     */
    @Test
    @DirtiesContext
    void deleteEventUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/events/1");

        EventUpdate update = null;
        completableFuture = new CompletableFuture<>();
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertNotNull(update, "update was null");
        assertEquals(EventUpdate.Action.DELETED_EVENT, update.getAction(), "Incorrect update " + update.getAction());
    }

    /**
     * DELETE api/events
     */
    @Test
    @DirtiesContext
    void deleteAllEventsUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        restTemplate.delete("/api/events");

        EventUpdate update = null;
        completableFuture = new CompletableFuture<>();
        try {
            update = completableFuture.get(5, SECONDS);
        } catch (TimeoutException t) {
            fail("Websocket detected no update");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertNotNull(update);
        assertEquals(EventUpdate.Action.DELETED_EVENT, update.getAction());
    }



    /**
     *
     */
    @Test
    void noUpdate() {
        StompSession session = getSession();

        session.subscribe("/api/events/websocket/1", new EventUpdateStompFrameHandler());

        try {
            completableFuture.get(5, SECONDS);
            fail(); // Should time out since there is no update
        } catch (Exception e) {
            // supposed to time out
        }
    }


}
