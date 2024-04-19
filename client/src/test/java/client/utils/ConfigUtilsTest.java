package client.utils;

import commons.Event;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ConfigUtilsTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ConfigUtils.file = tempDir.resolve("config.json").toFile();
    }

    @Test
    public void testTryLoadConfigFileExists() {
        String content = "{\"currency\":\"EUR\",\"exchangeRate\":0.88,\"prefferedLanguage\":\"French\"}";
        try (FileWriter writer = new FileWriter(ConfigUtils.file)) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ConfigUtils.tryLoadConfig();

        assertEquals("EUR", ConfigUtils.getCurrency());
        assertEquals(0.88, ConfigUtils.getExchangeRate());
        assertEquals("French", ConfigUtils.getPrefferedLanguage());
        assertTrue(ConfigUtils.file.delete(), "Failed to delete configuration file, possibly still open.");
    }

    @Test
    public void testTryLoadConfigFileNotExistsCreatesNew() {
        assertFalse(ConfigUtils.file.exists());

        ConfigUtils.tryLoadConfig();

        assertTrue(ConfigUtils.file.exists());
        assertEquals("USD", ConfigUtils.getCurrency());
        assertEquals(1.0, ConfigUtils.getExchangeRate());
        assertEquals("English", ConfigUtils.getPrefferedLanguage());
    }

    @Test
    public void testSaveConfig() throws Exception {
        ConfigUtils.tryLoadConfig();
        ConfigUtils.setCurrency("JPY", 110.5);

        String savedContent = new String(java.nio.file.Files.readAllBytes(ConfigUtils.file.toPath()));
        assertTrue(savedContent.contains("\"JPY\""));
        assertTrue(savedContent.contains("110.5"));
    }

    @Test
    public void testSetAndGetPrefferedLanguage() {
        ConfigUtils.tryLoadConfig();
        ConfigUtils.setPrefferedLanguage("Spanish");

        assertEquals("Spanish", ConfigUtils.getPrefferedLanguage());
    }

    @Test
    public void testSetAndGetServerUrl() {
        ConfigUtils.tryLoadConfig();
        ConfigUtils.setServerUrl("http://example.com");

        assertEquals("http://example.com", ConfigUtils.getServerUrl());
    }

    @Test
    public void testIsServerAvailableTrue() {
        ConfigUtils.client = mockClient(200);
        assertTrue(ConfigUtils.isServerAvailable("http://validserver.com"));
    }

    @Test
    public void testIsServerAvailableFalse() {
        ConfigUtils.client = mockClient(400);
        assertFalse(ConfigUtils.isServerAvailable("http://invalidserver.com"));
    }

    private static Client mockClient(int status) {
        Client mockedClient = Mockito.mock(Client.class);
        WebTarget mockWebTarget = Mockito.mock(WebTarget.class);
        Builder mockBuilder = Mockito.mock(Builder.class);
        Response mockResponse = Mockito.mock(Response.class);
        when(mockedClient.target(anyString())).thenReturn(mockWebTarget);
        when(mockWebTarget.path(anyString())).thenReturn(mockWebTarget);
        when(mockWebTarget.request(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.get()).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(status);
        return mockedClient;
    }

    @Test
    public void testSetAndGetUserEmail() {
        ConfigUtils.tryLoadConfig();
        ConfigUtils.setUserEmail("user@example.com");

        assertEquals("user@example.com", ConfigUtils.getUserEmail());
    }

    @Test
    public void testSetAndGetEmail() {
        ConfigUtils.tryLoadConfig();
        Event mockEvent = Mockito.mock(Event.class);
        when(mockEvent.getId()).thenReturn(1L);
        ConfigUtils.setCurrentEvent(mockEvent);
        ConfigUtils.setEmail(2L, "user2@example.com");

        assertEquals("user2@example.com", ConfigUtils.getEmail(2L));
    }

    @Test
    public void testSetAndGetIban() {
        ConfigUtils.tryLoadConfig();
        Event mockEvent = Mockito.mock(Event.class);
        when(mockEvent.getId()).thenReturn(1L);
        ConfigUtils.setCurrentEvent(mockEvent);
        ConfigUtils.setIban(2L, "NL20INGB0001234567");

        assertEquals("NL20INGB0001234567", ConfigUtils.getIban(2L));
    }

    @Test
    public void testSetAndGetEmailDifferentEvent() {
        ConfigUtils.tryLoadConfig();
        Event mockEvent = Mockito.mock(Event.class);
        when(mockEvent.getId()).thenReturn(1L);
        ConfigUtils.setCurrentEvent(mockEvent);
        ConfigUtils.setEmail(2L, "user2@example.com");

        when(mockEvent.getId()).thenReturn(2L); //switch to different event

        assertNull(ConfigUtils.getEmail(2L));
    }

    @Test
    public void testSetAndGetIbanDifferentEvent() {
        ConfigUtils.tryLoadConfig();
        Event mockEvent = Mockito.mock(Event.class);
        when(mockEvent.getId()).thenReturn(1L);
        ConfigUtils.setCurrentEvent(mockEvent);
        ConfigUtils.setIban(2L, "NL20INGB0001234567");

        when(mockEvent.getId()).thenReturn(2L); //switch to different event

        assertNull(ConfigUtils.getIban(2L));
    }
}