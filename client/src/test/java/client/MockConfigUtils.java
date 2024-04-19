package client;

import client.utils.ConfigUtils;
import commons.Event;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import jakarta.ws.rs.client.Invocation.Builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockConfigUtils {
    private static MockedStatic<ConfigUtils> config;
    private static Client mockedClient;

    public enum APIMethod {
        GET, POST, DELETE, PUT
    }

    private static final Map<String, WebTarget> urlMap = new HashMap<>();
    private static final Map<String, Map<String, WebTarget>> pathMap = new HashMap<>();

    /**
     * Used in context:
     * try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
     *     MockConfigUtils.init(config);
     *     // tests here
     * }
     */
    public static void init(MockedStatic<ConfigUtils> config) {
        urlMap.clear();
        pathMap.clear();
        MockConfigUtils.config = config;
        config.when(ConfigUtils::getCurrency).thenReturn("USD");
        config.when(ConfigUtils::getExchangeRate).thenReturn(1.0);
        config.when(ConfigUtils::getServerUrl).thenReturn("http://testserver.url/");
        config.when(() -> ConfigUtils.getIban(anyLong())).thenReturn(null);
        config.when(() -> ConfigUtils.getEmail(anyLong())).thenReturn(null);
        config.when(ConfigUtils::getUserEmail).thenReturn(null);
        config.when(ConfigUtils::getPrefferedLanguage).thenReturn("english");
        config.when(() -> ConfigUtils.isServerAvailable(anyString())).thenReturn(true);
        config.when(ConfigUtils::getEvents).thenReturn(new HashSet<>());
        ConfigUtils.client = mockClient();
    }

    private static Client mockClient() {
        mockedClient = Mockito.mock(Client.class);
        when(mockedClient.target(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return urlMap.get(url);
        });
        return mockedClient;
    }

    /**
     * mock an event which will be the currentEvent with id 1
     * @return the mocked event
     */
    public static Event mockCurrentEvent() {
        Event event = Mockito.mock(Event.class);
        when(event.getId()).thenReturn(1L);
        config.when(ConfigUtils::getCurrentEvent).thenReturn(event);
        return event;
    }

    public static void setIban(Long id, String iban) {
        config.when(() -> ConfigUtils.getIban(id)).thenReturn(iban);
    }

    public static void setEmail(Long id, String email) {
        config.when(() -> ConfigUtils.getEmail(id)).thenReturn(email);
    }

    public static void setUserEmail(String email) {
        config.when(ConfigUtils::getUserEmail).thenReturn(email);
    }

    public static void setEvents(Set<Long> events) {
        config.when(ConfigUtils::getEvents).thenReturn(events);
    }

    public static void setCurrency(String currency, Double conversion) {
        config.when(ConfigUtils::getCurrency).thenReturn(currency);
        config.when(ConfigUtils::getExchangeRate).thenReturn(conversion);
    }

    private static void ensureUrlAndPathAreMocked(String url, String path) {
        if (!urlMap.containsKey(url)) {
            WebTarget m = Mockito.mock(WebTarget.class);
            urlMap.put(url, m);
            pathMap.put(url, new HashMap<>());
            when(m.path(anyString())).thenAnswer(invocation -> {
                String pathArg = invocation.getArgument(0);
                return pathMap.get(url).get(pathArg);
            });
        }

        if (!pathMap.get(url).containsKey(path)) {
            WebTarget m = Mockito.mock(WebTarget.class);
            Builder b = Mockito.mock(Builder.class);
            when(m.request(MediaType.APPLICATION_JSON)).thenReturn(b);
            when(b.accept(MediaType.APPLICATION_JSON)).thenReturn(b);
            pathMap.get(url).put(path, m);
        }
    }

    private static <I> void mockRequest(Builder builder, APIMethod meth, I input, Response response) {
        switch (meth) {
            case GET -> when(builder.get()).thenReturn(response);
            case POST -> when(builder.post(Entity.entity(input == null ? any() : input, MediaType.APPLICATION_JSON))).thenReturn(response);
            case PUT -> when(builder.put(Entity.entity(input == null ? any() : input, MediaType.APPLICATION_JSON))).thenReturn(response);
            case DELETE -> when(builder.delete()).thenReturn(response);
        }
    }

    /**
     * Verify a certain endpoint is called
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     */
    public static <I> void verifyRequest(String url, String path, APIMethod meth, I input) {
        Builder builder = pathMap.get(url).get(path).request(MediaType.APPLICATION_JSON);
        switch (meth) {
            case GET -> verify(builder).get();
            case POST -> verify(builder).post(Entity.entity(input == null ? any() : input, MediaType.APPLICATION_JSON));
            case PUT -> verify(builder).put(Entity.entity(input == null ? any() : input, MediaType.APPLICATION_JSON));
            case DELETE -> verify(builder).delete();
        }
    }

    /**
     * Verify a certain endpoint is called
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     */
    public static void verifyRequest(String url, String path, APIMethod meth) {
        verifyRequest(url, path, meth, null);
    }

    /**
     * Verify a certain endpoint is called
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     */
    public static <I> void verifyRequest(String path, APIMethod meth, I input) {
        verifyRequest("http://testserver.url/", path, meth, input);
    }

    /**
     * Verify a certain endpoint is called
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     */
    public static <I> void verifyRequest(String path, APIMethod meth) {
        verifyRequest("http://testserver.url/", path, meth, null);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    private static <I, R> void mockEndPointImp(String url, String path, APIMethod meth, I input, Class<R> resultClass, R result) {
        Response response = Mockito.mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        if (resultClass != null) {
            when(response.readEntity(resultClass)).thenReturn(result);
        }

        ensureUrlAndPathAreMocked(url, path);
        mockRequest(pathMap.get(url).get(path).request(MediaType.APPLICATION_JSON), meth, input, response);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    private static <I, R> void mockEndPointImpGenT(String url, String path, APIMethod meth, I input,
                                                    GenericType<R> resultClass, R result) {
        Response response = Mockito.mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        if (resultClass != null) {
            when(response.readEntity(resultClass)).thenReturn(result);
        }

        ensureUrlAndPathAreMocked(url, path);
        mockRequest(pathMap.get(url).get(path).request(MediaType.APPLICATION_JSON), meth, input, response);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <I, R> void mockEndPoint(String url, String path, APIMethod meth, I input, Class<R> resultClass, R result) {
        mockEndPointImp(url, path, meth, input, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <I, R> void mockEndPoint(String url, String path, APIMethod meth, I input, GenericType<R> resultClass, R result) {
        mockEndPointImpGenT(url, path, meth, input, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/DELETE)
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <R> void mockEndPoint(String url, String path, APIMethod meth, Class<R> resultClass, R result) {
        mockEndPointImp(url, path, meth, null, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/DELETE)
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <R> void mockEndPoint(String url, String path, APIMethod meth, GenericType<R> resultClass, R result) {
        mockEndPointImpGenT(url, path, meth, null, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (POST/PUT)
     * @param input the posted/putted object
     */
    public static <I> void mockEndPoint(String url, String path, APIMethod meth, I input) {
        mockEndPointImp(url, path, meth, input, null, null);
    }

    /**
     * Mock an api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (DELETE)
     */
    public static void mockEndPoint(String url, String path, APIMethod meth) {
        mockEndPointImp(url, path, meth, null, null, null);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <I, R> void mockEndPoint(String path, APIMethod meth, I input, Class<R> resultClass, R result) {
        mockEndPointImp("http://testserver.url/", path, meth, input, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <I, R> void mockEndPoint(String path, APIMethod meth, I input, GenericType<R> resultClass, R result) {
        mockEndPointImpGenT("http://testserver.url/", path, meth, input, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/DELETE)
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <R> void mockEndPoint(String path, APIMethod meth, Class<R> resultClass, R result) {
        mockEndPointImp("http://testserver.url/", path, meth, null, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/DELETE)
     * @param resultClass the resulting class
     * @param result the result of the request
     */
    public static <R> void mockEndPoint(String path, APIMethod meth, GenericType<R> resultClass, R result) {
        mockEndPointImpGenT("http://testserver.url/", path, meth, null, resultClass, result);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (POST/PUT)
     * @param input the posted/putted object
     */
    public static <I> void mockEndPoint(String path, APIMethod meth, I input) {
        mockEndPointImp("http://testserver.url/", path, meth, input, null, null);
    }

    /**
     * Mock an api endpoint
     * @param path the relative path of the api request
     * @param meth the method (DELETE)
     */
    public static void mockEndPoint(String path, APIMethod meth) {
        mockEndPointImp("http://testserver.url/", path, meth, null, null, null);
    }

    /**
     * Mock a failed api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     */
    public static <I> void mockFailedEndPoint(String url, String path, APIMethod meth, I input, String errormsg) {
        Response response = Mockito.mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(errormsg);

        ensureUrlAndPathAreMocked(url, path);
        mockRequest(pathMap.get(url).get(path).request(MediaType.APPLICATION_JSON), meth, input, response);
    }

    /**
     * Mock a failed api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     * @param input the posted/putted object
     */
    public static <I> void mockFailedEndPoint(String path, APIMethod meth, I input, String errormsg) {
        mockFailedEndPoint("http://testserver.url/", path, meth, input, errormsg);
    }

    /**
     * Mock a failed api endpoint
     * @param url the url of the endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     */
    public static void mockFailedEndPoint(String url, String path, APIMethod meth, String errormsg) {
        mockFailedEndPoint(url, path, meth, null, errormsg);
    }

    /**
     * Mock a failed api endpoint
     * @param path the relative path of the api request
     * @param meth the method (GET/POST/PUT/DELETE)
     */
    public static void mockFailedEndPoint(String path, APIMethod meth, String errormsg) {
        mockFailedEndPoint("http://testserver.url/", path, meth, null, errormsg);
    }
}
