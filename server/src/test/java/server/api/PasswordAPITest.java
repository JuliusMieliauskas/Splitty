package server.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PasswordAPITest {
    @Autowired
    private TestRestTemplate restTemplate;
    static private HttpHeaders headers;

    @BeforeAll
    static void createHeaders() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void passwordCorrect() {
        // Normally this is only done in the main class,
        // calling it again will just overwrite the previous password
        String correctPass = PasswordController.generatePassword();
        HttpEntity<String> request = new HttpEntity<>(correctPass, headers);
        ResponseEntity<Boolean> response = restTemplate.postForEntity("/api/admin", request, Boolean.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody());
    }

    @Test
    void passwordIncorrect() {
        // Normally this is only done in the main class,
        // calling it again will just overwrite the previous password
        // Needs to be called here since, otherwise password seems to be null
        PasswordController.generatePassword();
        // Definitely not correct, since real password will be at least 20 characters
        String attempt = "mypassword";
        HttpEntity<String> request = new HttpEntity<>(attempt, headers);
        ResponseEntity<Boolean> response = restTemplate.postForEntity("/api/admin", request, Boolean.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody());
    }
}
