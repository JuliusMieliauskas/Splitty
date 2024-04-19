package client.utils;

import client.MockConfigUtils;
import commons.exceptions.FailedRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class CurrencyUtilsTest {
    @Test
    public void testGetExchangeRate() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/exchange-rate/USD/EUR", MockConfigUtils.APIMethod.GET, Double.class, 1.2);

            assertEquals(1.2, CurrencyUtils.getExchangeRate("EUR"));
        }
    }


    @Test
    public void testGetExchangeRateFailed() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            MockConfigUtils.init(config);
            MockConfigUtils.mockFailedEndPoint("api/exchange-rate/USD/BLAH", MockConfigUtils.APIMethod.GET, "error");

            assertThrows(FailedRequestException.class, () -> {
                CurrencyUtils.getExchangeRate("BLAH");
            });
        }
    }
}