package client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import commons.Event;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigUtils {
    public static Client client = ClientBuilder.newClient(new ClientConfig());
    private static Event currentEvent; // not stored in config, as it should reset when restarting the app
    private Double exchangeRate;
    private String currency;
    private boolean isAdmin;
    private String serverUrl;
    private String userEmail;
    private String emailPassword;
    private String prefferedLanguage;
    private Set<Long> events;
    private Map<Long, Map<Long, String>> emails; // key1: eventId; key2: userId
    private Map<Long, Map<Long, String>> ibans; // key1: eventId; key2: userId
    public static File file = new File("config.json"); // public for testing
    private static ConfigUtils configUtils;
    private static Gson gson;

    /**
     * Tries to load a config from the config file, if not successful, it will create a new config
     * @return The loaded/created config
     */
    public static void tryLoadConfig() {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        gson = builder.setPrettyPrinting().create();

        try (FileReader fr = new FileReader(file)) {
            configUtils = gson.fromJson(fr, ConfigUtils.class);
            if (configUtils != null) {
                return;
            }
        } catch (FileNotFoundException e) {
            // continue with constructing new ConfigUtils
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        configUtils = new ConfigUtils();
        save();
    }

    /**
     * Default constructor if no file is found
     */
    public ConfigUtils() {
        isAdmin = false;
        currency = "USD";
        prefferedLanguage = "English";
        exchangeRate = 1.0;
        emails = new HashMap<>();
        ibans = new HashMap<>();
        events = new HashSet<>();
    }

    private static void save() {
        try {
            String json = gson.toJson(configUtils);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPrefferedLanguage() {
        return configUtils.prefferedLanguage;
    }

    public static void setPrefferedLanguage(String prefferedLanguage) {
        configUtils.prefferedLanguage = prefferedLanguage;
        save();
    }

    public static Event getCurrentEvent() {
        return currentEvent;
    }

    public static void setCurrentEvent(Event currentEvent) {
        ConfigUtils.currentEvent = currentEvent;
    }

    public static Double getExchangeRate() {
        return configUtils.exchangeRate;
    }

    public static String getCurrency() {
        return configUtils.currency;
    }

    public static void setCurrency(String currency, Double exchangeRate) {
        configUtils.currency = currency;
        configUtils.exchangeRate = exchangeRate;
        save();
    }

    /**
     * Get the email of a given user
     */
    public static String getEmail(Long userId) {
        if (!configUtils.emails.containsKey(getCurrentEvent().getId())) {
            configUtils.emails.put(getCurrentEvent().getId(), new HashMap<>());
        }
        return configUtils.emails.get(getCurrentEvent().getId()).get(userId);
    }

    public static String getUserEmail() {
        return configUtils.userEmail;
    }
    public static String getEmailPassword() {
        return configUtils.emailPassword;
    }

    /**
     * Get the iban of a given user
     */
    public static String getIban(Long userId) {
        if (!configUtils.ibans.containsKey(getCurrentEvent().getId())) {
            configUtils.ibans.put(getCurrentEvent().getId(), new HashMap<>());
        }
        return configUtils.ibans.get(getCurrentEvent().getId()).get(userId);
    }

    public static void setUserEmail(String userEmail) {
        configUtils.userEmail = userEmail;
        save();
    }

    public static void setEmailPassword(String emailPassword) {
        configUtils.emailPassword = emailPassword;
        save();
    }

    /**
     * Set the email of a given user
     */
    public static void setEmail(Long userId, String mail) {
        if (!configUtils.emails.containsKey(getCurrentEvent().getId())) {
            configUtils.emails.put(getCurrentEvent().getId(), new HashMap<>());
        }
        configUtils.emails.get(getCurrentEvent().getId()).put(userId, mail);
        save();
    }

    /**
     * Set the iban of a given user
     */
    public static void setIban(Long userId, String iban) {
        if (!configUtils.ibans.containsKey(getCurrentEvent().getId())) {
            configUtils.ibans.put(getCurrentEvent().getId(), new HashMap<>());
        }
        configUtils.ibans.get(getCurrentEvent().getId()).put(userId, iban);
        save();
    }

    public static Set<Long> getEvents() {
        return configUtils.events;
    }

    public static void addEvent(Long id) {
        configUtils.events.add(id);
        save();
    }

    public static void removeEvent(Long id) {
        configUtils.events.remove(id);
        save();
    }

    public static boolean isAdmin() {
        return configUtils.isAdmin;
    }

    public static void setAdmin(boolean admin) {
        configUtils.isAdmin = admin;
        save();
    }

    public static String getServerUrl() {
        return configUtils.serverUrl;
    }

    public static void setServerUrl(String serverUrl) {
        configUtils.serverUrl = serverUrl;
        save();
    }

    /**
     * Checks if a certain server url is reachable
     */
    public static boolean isServerAvailable(String url) {
        try {
            return client.target(url).path("api/events").request(MediaType.APPLICATION_JSON).get().getStatus() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
