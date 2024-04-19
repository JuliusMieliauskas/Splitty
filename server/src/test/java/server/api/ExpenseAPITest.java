package server.api;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExpenseAPITest {
    @Autowired
    private TestRestTemplate restTemplate;
    static private HttpHeaders headers;

    @BeforeAll
    static void createHeaders() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * GET api/expenses
     */
    @Test
    void testGetAllExpenses() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/expenses", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(10, response.getBody().size());
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testAddNewExpense() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Event event = restTemplate.getForEntity("/api/events/1", Event.class).getBody();
        Expense expense = new Expense(10., "Test expense", user, event);
        HttpEntity<Expense> request = new HttpEntity<>(expense, headers);
        ResponseEntity<Expense> response = restTemplate.postForEntity("/api/expenses", request, Expense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getTitle(), expense.getTitle());
        assertEquals(response.getBody().getAmount(), expense.getAmount());
        assertEquals(response.getBody().getOriginalPayer(), expense.getOriginalPayer());
        assertEquals(response.getBody().getEvent(), expense.getEvent());
        assertNotNull(response.getBody().getId());
        assertEquals(
                LocalDateTime.now().truncatedTo(ChronoUnit.DAYS),
                response.getBody().getCreationDate().truncatedTo(ChronoUnit.DAYS)
        );
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testAddNewExpenseWorking() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Event event = restTemplate.getForEntity("/api/events/1", Event.class).getBody();
        Expense expense = new Expense(10., "Test expense", user, event);
        HttpEntity<Expense> request = new HttpEntity<>(expense, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/expenses", request, String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/expenses
     * GET api/expenses/{id}
     * Assumes GET api/users/{id} is working
     * Assumes GET api/events/{id} is working
     */
    @Test
    @DirtiesContext
    void testPostAndGet() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Event event = restTemplate.getForEntity("/api/events/1", Event.class).getBody();
        Expense expense = new Expense(10., "Test expense", user, event);
        HttpEntity<Expense> request = new HttpEntity<>(expense, headers);
        ResponseEntity<Expense> responsePost = restTemplate.postForEntity("/api/expenses", request, Expense.class);

        assertTrue(responsePost.getStatusCode().is2xxSuccessful());
        Expense addedExpense = responsePost.getBody();
        ResponseEntity<Expense> response = restTemplate.getForEntity("/api/expenses/" + addedExpense.getId(), Expense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(addedExpense, response.getBody());
    }

    /**
     * DELETE api/expenses
     * Assumes GET api/expenses is working
     */
    @Test
    @DirtiesContext
    void testDeleteAll() {
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/expenses", HttpMethod.DELETE, null, String.class);
        ResponseEntity<List> response = restTemplate.getForEntity("/api/expenses", List.class);
        assertTrue(response.getBody().isEmpty());
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/expenses/{id}
     */
    @Test
    void testGetExpense() {
        ResponseEntity<Expense> response = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Eggs", response.getBody().getTitle());
    }

    /**
     * GET api/expenses/{id}
     */
    @Test
    void testGetExpenseFail() {
        ResponseEntity<?> response = restTemplate.getForEntity("/api/expenses/200", null);
        assertFalse(response.getStatusCode().is2xxSuccessful());
    }

    /**
     * PUT api/expenses/{id}
     * Assumes GET /api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testPut() {
        ResponseEntity<Expense> responseGet = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        Expense modifiedExpense = responseGet.getBody();
        modifiedExpense.setTitle("New Eggs");
        HttpEntity<Expense> request = new HttpEntity<>(modifiedExpense, headers);
        restTemplate.put("/api/expenses/1", request, Expense.class);
        ResponseEntity<Expense> response = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("New Eggs", response.getBody().getTitle());
    }

    /**
     * PUT api/expenses/{id}
     * Assumes GET /api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testPutMaintainsOriginalPayer() {
        ResponseEntity<Expense> responseGet = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        Expense modifiedExpense = responseGet.getBody();
        User oldPayer = modifiedExpense.getOriginalPayer();
        assertNotNull(oldPayer);
        modifiedExpense.setTitle("New Eggs");
        HttpEntity<Expense> request = new HttpEntity<>(modifiedExpense, headers);
        restTemplate.put("/api/expenses/1", request, Expense.class);
        ResponseEntity<Expense> response = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        User newPayer = response.getBody().getOriginalPayer();
        assertEquals(oldPayer, newPayer);
    }

    /**
     * PUT api/expenses/{id}
     * Assumes GET /api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testPutFail() {
        ResponseEntity<Expense> responseGet = restTemplate.getForEntity("/api/expenses/1", Expense.class);
        assertTrue(responseGet.getStatusCode().is2xxSuccessful());
        Expense modifiedExpense = responseGet.getBody();
        modifiedExpense.setAmount(-10.);
        HttpEntity<Expense> request = new HttpEntity<>(modifiedExpense, headers);
        ResponseEntity<String> putResponse = restTemplate.exchange("/api/expenses/1", HttpMethod.PUT, request, String.class);
        assertFalse(putResponse.getStatusCode().is2xxSuccessful());
    }

    /**
     * DELETE api/expenses/{id}
     * Assumes GET /api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteExpense() {
        ResponseEntity<?> response1 = restTemplate.getForEntity("/api/expenses/1", null);
        assertTrue(response1.getStatusCode().is2xxSuccessful());
        restTemplate.delete("/api/expenses/1");
        ResponseEntity<?> response2 = restTemplate.getForEntity("/api/expenses/1", null);
        assertFalse(response2.getStatusCode().is2xxSuccessful());
    }

    /**
     * GET api/expenses/{id}/debtors
     */
    @Test
    void testGetAllDebtors() {
        ResponseEntity<List> response = restTemplate.getForEntity("/api/expenses/4/debtors", List.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(5, response.getBody().size());
    }

    /**
     * GET api/expenses/{id}/debtors
     */
    @Test
    void testGetAllDebtorsWorking() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/expenses/4/debtors", String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testAddNewDebtor() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Expense expense = restTemplate.getForEntity("/api/expenses/6", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<UserExpense> response = restTemplate.postForEntity("/api/expenses/6/debtors", request, UserExpense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(user, response.getBody().getDebtor());
        assertEquals(expense, response.getBody().getExpense());
        assertEquals(1., response.getBody().getPaidAmount());
        assertEquals(5., response.getBody().getTotalAmount());
        assertNotNull(response.getBody().getId());
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testAddNewDebtorWorking() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Expense expense = restTemplate.getForEntity("/api/expenses/6", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/expenses/6/debtors", request, String.class);
        assertSuccessful(response);
    }

    /**
     * POST api/expenses
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     * Assumes GET api/users/{id}/events is working
     */
    @Test
    @DirtiesContext
    void testAddNewDebtorUserMaintainsEvent() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        int oldAmount = restTemplate.getForEntity("/api/users/1/events", List.class).getBody().size();
        Expense expense = restTemplate.getForEntity("/api/expenses/6", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/expenses/6/debtors", request, String.class);
        assertSuccessful(response);
        int newAmount = restTemplate.getForEntity("/api/users/1/events", List.class).getBody().size();
        assertEquals(oldAmount, newAmount);
    }

    /**
     * POST api/expenses/{id}
     * GET api/expenses/{id}/debtors
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testPostAndGetDebtors() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Expense expense = restTemplate.getForEntity("/api/expenses/6", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<UserExpense> responsePost = restTemplate.postForEntity("/api/expenses/6/debtors", request, UserExpense.class);

        assertTrue(responsePost.getStatusCode().is2xxSuccessful());

        ResponseEntity<List<UserExpense>> response = restTemplate.exchange(
                "/api/expenses/6/debtors",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserExpense>>() { }
        );
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isEmpty());
        assertEquals(2, response.getBody().size());

        UserExpense addedUserExpense = responsePost.getBody();
        assertTrue(response.getBody().contains(addedUserExpense));
    }

    /**
     * POST api/expenses/{id}/debtors
     * GET api/expenses/{id}/debtors/{user_expense_id}
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testPostAndGetDebtor() {
        User user = restTemplate.getForEntity("/api/users/1", User.class).getBody();
        Expense expense = restTemplate.getForEntity("/api/expenses/6", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<UserExpense> responsePost = restTemplate.postForEntity("/api/expenses/6/debtors", request, UserExpense.class);

        assertTrue(responsePost.getStatusCode().is2xxSuccessful());
        UserExpense addedUserExpense = responsePost.getBody();

        ResponseEntity<UserExpense> response = restTemplate.getForEntity(
                "/api/expenses/6/debtors/" + addedUserExpense.getDebtor().getId(),
                UserExpense.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(addedUserExpense, response.getBody());
    }

    /**
     * GET api/expenses/{id}/debtors/{user_expense_id}
     */
    @Test
    void testGetDebtor() {
        ResponseEntity<UserExpense> response = restTemplate.getForEntity("/api/expenses/6/debtors/5", UserExpense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(10., response.getBody().getTotalAmount());
    }

    /**
     * GET api/expenses/{id}/debtors/{user_expense_id}
     */
    @Test
    void testGetDebtorWorking() {
        ResponseEntity<?> response = restTemplate.getForEntity("/api/expenses/6/debtors/5", null);
        assertSuccessful(response);
    }

    /**
     * PUT api/expenses/{id}/debtors/{user_expense_id}
     * Assumes GET api/expenses/{id}/debtors/{user_expense_id} is working
     */
    @Test
    @DirtiesContext
    void testPutDebtor() {
        UserExpense modifiedUserExpense = restTemplate.getForEntity("/api/expenses/1/debtors/1", UserExpense.class).getBody();
        modifiedUserExpense.setTotalAmount(1e9);
        HttpEntity<UserExpense> request = new HttpEntity<>(modifiedUserExpense, headers);
        restTemplate.put("/api/expenses/1/debtors/1", request, UserExpense.class);

        ResponseEntity<UserExpense> response = restTemplate.getForEntity("/api/expenses/1/debtors/1", UserExpense.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1e9, response.getBody().getTotalAmount());
    }

    /**
     * PUT api/expenses/{id}/debtors/{user_expense_id}
     * Assumes GET api/expenses/{id}/debtors/{user_expense_id} is working
     */
    @Test
    @DirtiesContext
    void testPutDebtorWorking() {
        UserExpense modifiedUserExpense = restTemplate.getForEntity("/api/expenses/1/debtors/1", UserExpense.class).getBody();
        modifiedUserExpense.setTotalAmount(1e9);
        HttpEntity<UserExpense> request = new HttpEntity<>(modifiedUserExpense, headers);
        ResponseEntity<String> responsePut = restTemplate.exchange("/api/expenses/1/debtors/1", HttpMethod.PUT, request, String.class);
        assertSuccessful(responsePut);
    }

    /**
     * DELETE api/expenses/{id}/debtors/{user_expense_id}
     * Assumes GET api/expenses/{id}/debtors is working
     * Assumes GET api/expenses/{id}/debtors/{user_expense_id} is working
     */
    @Test
    @DirtiesContext
    void testDeleteDebtor() {
        restTemplate.delete("/api/expenses/1/debtors/1", UserExpense.class);

        ResponseEntity<?> response = restTemplate.getForEntity("/api/expenses/1/debtors/1", null);
        assertFalse(response.getStatusCode().is2xxSuccessful());
        ResponseEntity<List> response2 = restTemplate.getForEntity("/api/expenses/1/debtors", List.class);
        assertEquals(0, response2.getBody().size());
    }

    /**
     * DELETE api/expenses/{id}/debtors/{user_expense_id}
     */
    @Test
    @DirtiesContext
    void testDeleteDebtorWorking() {
        ResponseEntity<String> responsePut = restTemplate.exchange("/api/expenses/1/debtors/1", HttpMethod.DELETE, null, String.class);
        assertSuccessful(responsePut);
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
