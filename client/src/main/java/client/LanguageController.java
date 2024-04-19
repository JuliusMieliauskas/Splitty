package client;

import client.utils.ConfigUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class LanguageController {
    private static LanguageController instance;
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;

    private LanguageController() {
        setLocale(new Locale(ConfigUtils.getPrefferedLanguage().toLowerCase()));
    }

    /**
     * Get the instance of the LanguageController
     */
    public static LanguageController getInstance() {
        if (instance == null) {
            instance = new LanguageController();
        }
        return instance;
    }

    /**
     * Set the locale of the LanguageController
     */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;

        String path = "src/main/resources/client/UIResources/language_" +
                locale + ".properties";
        path = ((new File(path)).exists() ? path : "client/" + path);
        try (InputStream stream = new FileInputStream(path)) {
            this.resourceBundle = new PropertyResourceBundle(stream);
        } catch (Exception e) {
            System.out.println("Error in loading bundle: " + e);
        }
    }

    public String getString(String key) {
        return resourceBundle.getString(key);
    }

    public Locale getCurrentLocale() {
        return this.currentLocale;
    }

    /**
     * Get the available languages from the file system
     */
    public static List<String> getAvailableLanguages() {
        // read the directory from the path and judging by the filename return only the title of the language
        String path = "src/main/resources/client/UIResources/";
        path = ((new File(path)).exists() ? path : "client/" + path);
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return new ArrayList<>();
        }
        List<String> languages = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String languageTitle = listOfFiles[i].getName().substring(8, listOfFiles[i].getName().length() - 11);
                if (languageTitle.isEmpty() || languageTitle.isBlank()) {
                    continue;
                }
                languages.add(capitalizedLocale(languageTitle.substring(1)));
            }
        }
        return languages;
    }

    /**
     * Capitalize the first letter of the current locale
     */
    public static String capitalizedLocale() {
        return currentLocale.getDisplayLanguage().substring(0, 1).toUpperCase() + currentLocale.getDisplayLanguage().substring(1);
    }

    /**
     * capitalize the first letter of the locale
     */
    public static String capitalizedLocale(String locale) {
        return locale.substring(0, 1).toUpperCase() + locale.substring(1);
    }

    /**
     * Add a new language to the client
     * @param title
     * @param dictionary
     * @throws Exception
     */
    public static void addNewLanguage(String title, String dictionary) throws Exception {
        if (title.isEmpty()) {
            throw new Exception("Title cannot be empty");
        }
        String[] lines = dictionary.split("\n");
        Properties properties = new Properties();
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length != 2 || parts[1].isEmpty() || parts[1].isBlank()) {
                throw new Exception("Invalid dictionary format. " +
                        "The value for key \"" + parts[0] + "\" is empty.");
            }
            properties.setProperty(parts[0], parts[1]);
        }

        String path = "src/main/resources/client/UIResources/language_" +
                title.toLowerCase() + ".properties";
        path = ((new File("src/main/resources/client/UIResources/")).exists() ? path : "client/" + path);
        // If file already exists throw error
        if (Files.exists(Path.of(path))) {
            throw new Exception("This language already exists");
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.ISO_8859_1)) {
            properties.store(writer, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Successfully saved a new language");
    }
}