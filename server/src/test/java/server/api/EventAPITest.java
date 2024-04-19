package server.api;

import commons.Event;
import commons.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EventAPITest {
    @Autowired
    private TestRestTemplate restTemplate;
    static private HttpHeaders headers;

    @BeforeAll
    static void createHeaders() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void testGetJsonWorking() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/events/1/json", String.class);
        assertSuccessful(response);
        assertFalse(response.getBody() == null || response.getBody().isEmpty());
        System.out.println(response.getBody());
    }

    /**
     * GET api/events
     */
    @Test
    void testGetAllEvents() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/events", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(3, response.getBody().size());
    }

    /**
     * POST api/events
     */
    @Test
    @DirtiesContext
    void testPostEvent() {
        Event event = new Event("cooking clas 2");
        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<Event> response = restTemplate.postForEntity("/api/events", request, Event.class);
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(response.getBody().getTitle(), event.getTitle());
        assertNotNull(response.getBody().getInviteCode());
        assertNotNull(response.getBody().getId());
        assertEquals(
                LocalDateTime.now().truncatedTo(ChronoUnit.DAYS),
                response.getBody().getCreationDate().truncatedTo(ChronoUnit.DAYS)
        );
    }

    /**
     * POST api/events
     */
    @Test
    @DirtiesContext
    void testPostEventWorking() {
        Event event = new Event("cooking clas 2");
        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/events", request, String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/events
     * Assumes GET api/events/{id}/users is working
     */
    @Test
    @DirtiesContext
    void testPostEventCreatorJoins() {
        Event event = new Event("cooking clas 2");
        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<Event> response = restTemplate.postForEntity("/api/events", request, Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        ResponseEntity<List<User>> usersResponse = restTemplate.exchange(
                "/api/events/" + response.getBody().getId() + "/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<User>>() { }
        );
        assertTrue(usersResponse.getStatusCode().is2xxSuccessful());
        List<User> users = usersResponse.getBody();
        assertEquals(0, users.size());
    }

    /**
     * DELETE api/events
     * Assumes GET api/events is working
     */
    @Test
    @DirtiesContext
    void testDeleteAll() {
        restTemplate.delete("/api/events");
        ResponseEntity<List> response = restTemplate.getForEntity("/api/events", List.class);
        assertTrue(response.getBody().isEmpty());
    }

    /**
     * GET api/events/{id}
     */
    @Test
    void testGetEventById() {
        ResponseEntity<Event> response = restTemplate.getForEntity("/api/events/2", Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(response.getBody().getTitle(), "OWEE 2023");
    }

    /**
     * GET api/events/{id}
     */
    @Test
    void testGetEventByInvalidId() {
        ResponseEntity<?> response = restTemplate.getForEntity("/api/events/200", null);
        assertFalse(response.getStatusCode().is2xxSuccessful());
    }

    /**
     * POST api/events
     * GET api/events/{id}
     */
    @Test
    @DirtiesContext
    void testPostAndGet() {
        Event event = new Event("cooking clas 2");
        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<Event> responsePost = restTemplate.postForEntity("/api/events", request, Event.class);
        assertTrue(responsePost.getStatusCode().is2xxSuccessful());
        Event addedEvent = responsePost.getBody();
        ResponseEntity<Event> response = restTemplate.getForEntity("/api/events/" + addedEvent.getId(), Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(addedEvent, response.getBody());
    }

    /**
     * PUT api/events/{id}
     * Assumes GET /api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testPut() {
        ResponseEntity<Event> responseGet = restTemplate.getForEntity("/api/events/1", Event.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        Event modifiedEvent = responseGet.getBody();
        modifiedEvent.setTitle("New title");
        HttpEntity<Event> request = new HttpEntity<>(modifiedEvent, headers);
        restTemplate.put("/api/events/1", request, Event.class);
        ResponseEntity<Event> response = restTemplate.getForEntity("/api/events/1", Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("New title", response.getBody().getTitle());
    }

    /**
     * PUT api/events/{id}
     * Assumes GET /api/events/{id} is working
     * Assumes GET /api/events/{id}/users is working
     */
    @Test
    @DirtiesContext
    void testPutMaintainsUsers() {
        ResponseEntity<Event> responseGet = restTemplate.getForEntity("/api/events/1", Event.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        Event modifiedEvent = responseGet.getBody();
        modifiedEvent.setTitle("New title");
        int oldAmount = restTemplate.getForEntity("/api/events/1/users", List.class).getBody().size();
        HttpEntity<Event> request = new HttpEntity<>(modifiedEvent, headers);
        restTemplate.put("/api/events/1", request, Event.class);
        ResponseEntity<Event> response = restTemplate.getForEntity("/api/events/1", Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        int newAmount = restTemplate.getForEntity("/api/events/1/users", List.class).getBody().size();
        assertEquals(oldAmount, newAmount);

    }

    /**
     * DELETE api/events/{id}
     * Assumes GET /api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteEvent() {
        ResponseEntity<?> response1 = restTemplate.getForEntity("/api/events/1", null);
        assertTrue(response1.getStatusCode().is2xxSuccessful());
        restTemplate.delete("/api/events/1");
        ResponseEntity<?> response2 = restTemplate.getForEntity("/api/events/1", null);
        assertFalse(response2.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/events/{id}/users
     */
    @Test
    void testGetAllUsersFromEvent() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/events/1/users", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(2, response.getBody().size());
    }

    /**
     * POST api/events/{id}/users
     * Assumes GET api/events/{id}/users is working
     * Assumes GET api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void testAddUserToEvent() {
        User user = restTemplate.getForEntity("/api/users/6", User.class).getBody();
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> responsePOST = restTemplate.postForEntity("/api/events/1/users", request, String.class);
        assertTrue(responsePOST.getStatusCode().is2xxSuccessful());

        ResponseEntity<List<User>> response = restTemplate.exchange(
                "/api/events/1/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<User>>() { }
        );
        System.out.println(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().contains(user));
    }

    /**
     * POST api/events/{id}/users
     */
    @Test
    @DirtiesContext
    void testAddUserToEventWorking() {
        User user = restTemplate.getForEntity("/api/users/6", User.class).getBody();
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/events/1/users", request, String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/events/{id}/users
     * Assumes Get api/users/{id}/events is working
     */
    @Test
    @DirtiesContext
    void testAddUserToEventUserMaintainsEvents() {
        User user = restTemplate.getForEntity("/api/users/6", User.class).getBody();
        int oldAmount = restTemplate.getForEntity("/api/users/6/events", List.class).getBody().size();
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/events/1/users", request, String.class);
        assertSuccessful(response);
        int newAmount = restTemplate.getForEntity("/api/users/6/events", List.class).getBody().size();
        assertEquals(oldAmount + 1, newAmount);
    }


    /**
     * DELETE api/events/{e_id}/users/{u_id}
     * Assumes GET /api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteUserFromEvent() {
        restTemplate.delete("/api/events/1/users/2");
        ResponseEntity<List<User>> response = restTemplate.exchange(
                "/api/events/1/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<User>>() { }
        );
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().size());
        assertNotEquals(3, response.getBody().get(0).getId());
    }

    /**
     * DELETE api/events/{e_id}/users/{u_id}
     */
    @Test
    @DirtiesContext
    void testDeleteUserFromEventFail() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/events/1/users/200",
                HttpMethod.DELETE,
                null,
                String.class
        );
        assertFalse(response.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/events/{id}/expenses
     */
    @Test
    void testGetAllExpensesFromEvent() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/events/1/expenses", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(3, response.getBody().size());
    }

    /**
     * GET api/events/invite-code/{code}
     * Assumes GET api/events/{id} is working
     */
    @Test
    void testGetByInviteCode() {
        ResponseEntity<Event> responseCode = restTemplate.getForEntity("/api/events/invite-code/999999", Event.class);
        assertTrue(responseCode.getStatusCode().is2xxSuccessful());
        ResponseEntity<Event> response = restTemplate.getForEntity("/api/events/1", Event.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(response.getBody(), responseCode.getBody());
    }


    private void assertSuccessful(ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            if (response.hasBody() && response.getBody() instanceof String) {
                fail((String)response.getBody());
            } else {
                fail(Integer.toString(response.getStatusCode().value()));
            }
        }
    }
}
