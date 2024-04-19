package client.utils;

import client.MockConfigUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class WebSocketUtilsTest {
    // tests for a working websocket connection are on server side
    @Test
    public void testOpenSocketUnsuccessful() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)
        ) {
            MockConfigUtils.init(config);
            assertThrows(RuntimeException.class, () -> {
                new WebSocketUtils(ConfigUtils.getServerUrl(), null);
            });
        }
    }

}