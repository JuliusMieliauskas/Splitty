package server.api;

import commons.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserAPITest {
    @Autowired
    private TestRestTemplate restTemplate;
    static private HttpHeaders headers;

    @BeforeAll
    static void createHeaders() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * GET api/users
     */
    @Test
    void testGetAllUsers() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/users", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(13, response.getBody().size());
    }

    /**
     * POST api/users
     */
    @Test
    @DirtiesContext
    void testAddNewUser() {
        User user = new User("mymail@tudelft.nl", "NL93RABO3730976796", "Jeroen");
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<User> response = restTemplate.postForEntity("/api/users", request, User.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(null, response.getBody().getEmail());
        assertEquals(null, response.getBody().getIban());
        assertEquals(response.getBody().getUsername(), user.getUsername());
        assertNotNull(response.getBody().getId());
    }

    /**
     * POST api/users
     */
    @Test
    @DirtiesContext
    void testAddNewUserWorking() {
        User user = new User("mymail@tudelft.nl", "NL93RABO3730976796", "Jeroen");
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request, String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/users
     * GET api/users/{id}
     */
    @Test
    @DirtiesContext
    void testPostAndGet() {
        User user = new User("mymail@tudelft.nl", "NL93RABO3730976796", "Jeroen");
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<User> responsePost = restTemplate.postForEntity("/api/users", request, User.class);
        assertTrue(responsePost.getStatusCode().is2xxSuccessful());
        User addedUser = responsePost.getBody();
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/" + addedUser.getId(), User.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(addedUser, response.getBody());
    }

    /**
     * DELETE api/users
     * Assumes GET api/users is working
     */
    @Test
    @DirtiesContext
    void testDeleteAllFail() {
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/users", HttpMethod.DELETE, null, String.class);
        ResponseEntity<List> response = restTemplate.getForEntity("/api/users", List.class);
        assertFalse(response.getBody().isEmpty());
        // Should not be possible since users still owns event and have expenses
        assertFalse(deleteResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * DELETE api/users
     * Assumes GET api/users is working
     * Assumes DELETE api/events is working
     * Assumes DELETE api/expenses is working
     */
    @Test
    @DirtiesContext
    void testDeleteAll() {
        restTemplate.delete("/api/events");
        restTemplate.delete("/api/expenses");
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/users", HttpMethod.DELETE, null, String.class);
        ResponseEntity<List> response = restTemplate.getForEntity("/api/users", List.class);
        assertTrue(response.getBody().isEmpty());
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/users/{id}
     */
    @Test
    void testGetUser() {
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("walter", response.getBody().getUsername());
    }

    /**
     * PUT api/users/{id}
     * Assumes GET /api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void testPut() {
        ResponseEntity<User> responseGet = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        User modifiedUser = responseGet.getBody();
        modifiedUser.setUsername("New Walter");
        HttpEntity<User> request = new HttpEntity<>(modifiedUser, headers);
        restTemplate.put("/api/users/1", request, User.class);
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("new walter", response.getBody().getUsername());
    }

    /**
     * PUT api/users/{id}
     * Assumes GET /api/users/{id} is working
     * Assumes GET /api/users/{id}/events is working
     */
    @Test
    @DirtiesContext
    void testPutMaintainsEvents() {
        ResponseEntity<User> responseGet = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        User modifiedUser = responseGet.getBody();
        modifiedUser.setUsername("New Walter");
        int oldAmount = restTemplate.getForEntity("/api/users/1/events", List.class).getBody().size();
        HttpEntity<User> request = new HttpEntity<>(modifiedUser, headers);
        restTemplate.put("/api/users/1", request, User.class);
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        int newAmount = restTemplate.getForEntity("/api/users/1/events", List.class).getBody().size();
        assertEquals(oldAmount, newAmount);
    }

    /**
     * DELETE api/users/{id}
     * Assumes GET /api/users/{id} is working
     * Assumes DELETE api/expenses is working
     */
    @Test
    @DirtiesContext
    void testDeleteUser() {
        restTemplate.delete("/api/expenses");
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/2", User.class);
        assertSuccessful(response);
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/users/2", HttpMethod.DELETE, null, String.class);
        assertSuccessful(deleteResponse);
        ResponseEntity<String> response2 = restTemplate.getForEntity("/api/users/2", String.class);
        assertFalse(response2.getStatusCode().is2xxSuccessful());
    }

    /**
     * DELETE api/users/{id}
     * Assumes GET /api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteUserFail() {
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertSuccessful(response);
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/users/1", HttpMethod.DELETE, null, String.class);
        assertFalse(deleteResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * DELETE api/users/{id}
     * Assumes GET /api/users/{id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteUserFailExpense() {
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/2", User.class);
        assertSuccessful(response);
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/users/2", HttpMethod.DELETE, null, String.class);
        assertFalse(deleteResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/users/{id}/events
     */
    @Test
    void testGetAllEventsFromUser() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/users/1/events", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(1, response.getBody().size());
    }

    /**
     * GET api/users/{id}/expenses
     */
    @Test
    void testGetAllExpensesFromUser() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/users/1/expenses", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(1, response.getBody().size());
    }

    /**
     * GET api/users/username/{username}
     * Assumes GET api/users/{id} is working
     */
    @Test
    void testGetByUsername() {
        ResponseEntity<User> responseUname = restTemplate.getForEntity("/api/events/1/username/WaLtER", User.class);
        assertTrue(responseUname.getStatusCode().is2xxSuccessful());
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertTrue(responseUname.getStatusCode().is2xxSuccessful());
        assertEquals(response.getBody(), responseUname.getBody());
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
