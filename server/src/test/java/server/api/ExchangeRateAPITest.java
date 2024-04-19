package server.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExchangeRateAPITest {
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Tests if the API works
     */
    @Test
    void testValidExchangeRate() {
        ResponseEntity<Double> responseUsdEur = restTemplate.getForEntity("/api/exchange-rate/USD/EUR", Double.class);
        ResponseEntity<Double> responseEurUsd = restTemplate.getForEntity("/api/exchange-rate/EUR/USD", Double.class);
        assertTrue(responseUsdEur.getStatusCode().is2xxSuccessful());
        assertTrue(responseEurUsd.getStatusCode().is2xxSuccessful());
        double rateUsdEur = responseUsdEur.getBody();
        double rateEurUsd = responseEurUsd.getBody();
        assertTrue( Math.abs(1 / rateUsdEur - rateEurUsd) < 1e-4);
    }

    /**
     * Tests if the API correctly returns invalid response
     */
    @Test
    void testInvalidExchangeRate() {
        ResponseEntity<?> response = restTemplate.getForEntity("/api/exchange-rate/EUR/blah", null);
        assertFalse(response.getStatusCode().is2xxSuccessful());
    }
}
