package server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.List;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LongPollingTest {
    @Autowired
    private TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    static private HttpHeaders headers;
    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void createHeaders() {
        System.out.println();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * GET api/expenses/{id}/debtors/long-poll
     * Assumes POST api/expenses/{id}/debtors is working
     * Assumes GET api/users/{id} is working
     * Assumes GET api/expenses/{id} is working
     */
    @Test
    @DirtiesContext
    void testLongPollingAddDebtor() throws Exception {
        MvcResult asyncListener = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/expenses/5/debtors/long-poll"))
                .andExpect(request().asyncStarted())
                .andReturn();

        User user = restTemplate.getForEntity("/api/users/2", User.class).getBody();
        Expense expense = restTemplate.getForEntity("/api/expenses/5", Expense.class).getBody();
        UserExpense userExpense = new UserExpense(user, expense, 1., 5.);
        HttpEntity<UserExpense> request = new HttpEntity<>(userExpense, headers);
        ResponseEntity<String> responsePOST = restTemplate.postForEntity("/api/expenses/5/debtors", request, String.class);
        assertTrue(responsePOST.getStatusCode().is2xxSuccessful());

        MockHttpServletResponse response;
        try {
            response = mockMvc
                    .perform(asyncDispatch(asyncListener))
                    .andReturn()
                    .getResponse();
        } catch (IllegalStateException e) {
            fail(); // The request timed out
            return; // <- so IntelliJ doesn't complain about uninitialized variable
        }
        int status = response.getStatus();
        assertEquals(200, status);
        String responseString = response.getContentAsString();
        List result = objectMapper.readValue(responseString, List.class);
        assertEquals(3, result.size());
    }

    /**
     * GET api/expenses/{id}/debtors/long-poll
     * Assumes DELETE api/expenses/{eid}/debtors/{uid} is working
     */
    @Test
    @DirtiesContext
    void testLongPollingRemoveDebtor() throws Exception {
        MvcResult asyncListener = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/expenses/5/debtors/long-poll"))
                .andExpect(request().asyncStarted())
                .andReturn();

        restTemplate.delete("/api/expenses/5/debtors/5", UserExpense.class);

        MockHttpServletResponse response;
        try {
            response = mockMvc
                    .perform(asyncDispatch(asyncListener))
                    .andReturn()
                    .getResponse();
        } catch (IllegalStateException e) {
            fail(); // The request timed out
            return; // <- so IntelliJ doesn't complain about uninitialized variable
        }
        int status = response.getStatus();
        assertEquals(200, status);
        String responseString = response.getContentAsString();
        List result = objectMapper.readValue(responseString, List.class);
        assertEquals(1, result.size());
    }

    /**
     * GET api/expenses/{id}/debtors/long-poll
     * Assumes GET api/expenses/{eid}/debtors/{uid} is working
     * Assumes PUT api/expenses/{eid}/debtors/{uid} is working
     */
    @Test
    @DirtiesContext
    void testLongPollingUpdateDebtor() throws Exception {
        MvcResult asyncListener = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/expenses/5/debtors/long-poll"))
                .andExpect(request().asyncStarted())
                .andReturn();

        UserExpense modifiedUserExpense = restTemplate.getForEntity("/api/expenses/5/debtors/5", UserExpense.class).getBody();
        modifiedUserExpense.setTotalAmount(1e9);
        HttpEntity<UserExpense> request = new HttpEntity<>(modifiedUserExpense, headers);
        restTemplate.exchange("/api/expenses/5/debtors/5", HttpMethod.PUT, request, String.class);

        MockHttpServletResponse response;
        try {
            response = mockMvc
                    .perform(asyncDispatch(asyncListener))
                    .andReturn()
                    .getResponse();
        } catch (IllegalStateException e) {
            fail(); // The request timed out
            return; // <- so IntelliJ doesn't complain about uninitialized variable
        }
        int status = response.getStatus();
        assertEquals(200, status);
        String responseString = response.getContentAsString();
        List result = objectMapper.readValue(responseString, List.class);
        assertEquals(2, result.size());
    }

    /**
     * GET api/expenses/5/debtors/long-poll
     */
    @Test
    void testLongPollingNoResult() throws Exception {
        MvcResult asyncListener = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/expenses/5/debtors/long-poll"))
                .andExpect(request().asyncStarted())
                .andReturn();


        try {
            mockMvc
                    .perform(asyncDispatch(asyncListener))
                    .andReturn()
                    .getResponse();
            fail(); // got a result before the timeout
        } catch (IllegalStateException e) {
            // long-polling timed out (10s), because no change where made to the user in event
        }
    }
}
