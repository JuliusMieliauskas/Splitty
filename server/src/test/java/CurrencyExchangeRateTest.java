import com.fasterxml.jackson.core.JsonProcessingException;
import commons.exceptions.FailedExchangeRateConversionException;
import commons.exceptions.InvalidFileFormatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.CurrencyExchangeRate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CurrencyExchangeRateTest {

    /**
     * Deletes the files created after each test using deleteDirectories().
     */
    @AfterEach
    void deleteCreation() {
        File file = new File("rates");
        if (file.exists()) {
            deleteDirectories(file);
        }
    }

    /**
     * Deletes files created after each test
     * @param file the file that's going to be deleted.
     */
    void deleteDirectories(File file) {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subfile : files) {
                    deleteDirectories(subfile);
                }
            }

        }
        file.delete();
    }
    /**
     * Testes whether exception is thrown when parameters are invalid
     */
    @Test
    void testNullCurrencies() {
        assertThrows(FailedExchangeRateConversionException.class, () -> CurrencyExchangeRate.exchangeRateFromTo(null, null));
    }
    /**
     * Tests whether exception is thrown if the parameters are invalid.
     */
    @Test
    void testInvalidCurrencies() {
        assertThrows(FailedExchangeRateConversionException.class, () -> CurrencyExchangeRate.exchangeRateFromTo("RAN", "DOM"));
    }
    /**
     * Tests to confirm no exception is thrown if the parameters are valid.
     */
    @Test
    void testCorrectCurrencies() {
        assertDoesNotThrow(() -> CurrencyExchangeRate.exchangeRateFromTo("USD", "CHF"));
    }

    /**
     * Tests if the resulting exchange rate is a positive number, because exchange rates must all be positive.
     */
    @Test
    void testValidExchangeRate() {
        try {
            double exchangeRateUsdToEur = CurrencyExchangeRate.exchangeRateFromTo("USD", "EUR");
            double exchangeRateEurToUsd = CurrencyExchangeRate.exchangeRateFromTo("EUR", "USD");
            assertTrue(exchangeRateUsdToEur > 0);
            assertTrue(Math.abs(1 / exchangeRateUsdToEur - exchangeRateEurToUsd) < 1e-4); // float compare
        } catch (FailedExchangeRateConversionException e) {
            throw new RuntimeException(); // Shouldn't happen, if it happens it is not a problem with the program
        }
    }
    /**
     * Tests whether exchange rate equals 1 if a currency is converted to itself.
     */
    @Test
    void testValidExchangeRateSelf() {
        try {
            assertEquals(1.0, CurrencyExchangeRate.exchangeRateFromTo("EUR", "EUR"));
        } catch (FailedExchangeRateConversionException e) {
            throw new RuntimeException(); // Shouldn't happen, if it happens it is not a problem with the program
        }
    }

    /**
     * Tests whether readResponse parse a valid JSON.
     *
     * @throws JsonProcessingException If anything wrong happens while parsing.
     */
    @Test
    void testValidResponseParsing() throws JsonProcessingException {
        String validJson = """
                {
                    "data": {
                        "EUR": 1.5,
                        "CHF": 1
                    }
                }""";
        try {
            double exchangeRateEurToChf = CurrencyExchangeRate.fromJSON(validJson, "EUR", "CHF");
            double exchangeRateChfToEur = CurrencyExchangeRate.fromJSON(validJson, "CHF", "EUR");
            assertEquals(1 / 1.5, exchangeRateEurToChf);
            assertEquals(1.5, exchangeRateChfToEur);
        } catch (FailedExchangeRateConversionException e) {
            fail();
        }
    }
    /**
     * Tests whether an exception gets thrown if unexpected arguments show up in JSON response.
     */
    @Test
    void testInvalidResponseParsing() {
        String invalidJson = """
                {
                    "data": {
                        "EUR": HelloThere,
                        "CHF": 1.5
                    }
                }""";
        assertThrows(JsonProcessingException.class, () -> CurrencyExchangeRate.fromJSON(invalidJson, "EUR", "CHF"));
    }

    /**
     * Tests whether an exception gets thrown if the wrong currencies show up in JSON response.
     */
    @Test
    void testInvalidCurrInJson() {
        String invalidJson = """
                {
                    "data": {
                        "USD": 1.0,
                        "CHF": 1.5
                    }
                }""";
        assertThrows(FailedExchangeRateConversionException.class, () -> CurrencyExchangeRate.fromJSON(invalidJson, "EUR", "CHF"));
    }

    /**
     * Tests whether file gets created after exchange rate is calculated.
     */
    @Test
    void testFileCreated() {
        CurrencyExchangeRate.getFile("USD", "EUR", true);
        String currentDate = LocalDate.now().toString();
        assertTrue(new File("rates/" + currentDate + "/USD/EUR.txt").isFile());
    }

    /**
     * Tests whether file gets created after exchange rate is calculated.
     */
    @Test
    void testFileNotCreatedInvalidCurrency() {
        try {
            CurrencyExchangeRate.exchangeRateFromTo("BING", "BONG");
            fail(); // somehow successful
        } catch (FailedExchangeRateConversionException e) {
            // correct behaviour
        }
        String currentDate = LocalDate.now().toString();
        assertFalse(new File("rates/" + currentDate + "/BING/BONG.txt").isFile());
    }

    /**
     * Tests whether the returned exchange rate is the same as the cached exchange rate
     * @throws IOException if the parameters are invalid.
     */
    @Test
    void testFileSameCurrency() throws IOException {
        double exchangeRateChfToChf = 0;
        try {
            exchangeRateChfToChf = CurrencyExchangeRate.exchangeRateFromTo("EUR", "CHF");
        } catch (FailedExchangeRateConversionException e) {
            throw new RuntimeException(e);
        }
        String currentDate = LocalDate.now().toString();
        File file = new File("rates/" + currentDate + "/EUR/CHF.txt");
        double cachedExchangeRateChfToChf = 0;
        try {
            cachedExchangeRateChfToChf = CurrencyExchangeRate.readFromFile(file);
        } catch (InvalidFileFormatException e) {
            fail();
        }
        assertEquals(exchangeRateChfToChf, cachedExchangeRateChfToChf);
    }

}
