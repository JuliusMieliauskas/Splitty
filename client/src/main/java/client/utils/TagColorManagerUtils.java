package client.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TagColorManagerUtils {
    public static String TAG_COLORS_DIRECTORY = (
            (new File("src/main/resources/client/UIResources/")).exists() ?
            "src/main/resources/client/tagColors/" :
            "client/src/main/resources/client/tagColors/");
    public static String TAG_COLORS_FILE_EXTENSION = "tagColors.txt";

    /**
     * reads tag color combinations from file
     *
     * @param eventId tags and colors related to this event will be read
     * @return tag color combinations
     */
    public static Map<String, String> readTagColorsFromFile(Long eventId) {
        Map<String, String> tagColors = new HashMap<>();
        File file = new File(TAG_COLORS_DIRECTORY + eventId + TAG_COLORS_FILE_EXTENSION);
        File directory = new File(TAG_COLORS_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String tag = parts[0];
                        String color = parts[1].trim().equals("null") ? null : parts[1].trim();
                        tagColors.put(tag, color);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("The tagColors.txt file for event " + eventId + " was not found. ");
        }
        return tagColors;
    }

    /**
     * writes tag color combinations to a txt file
     * @param eventId tags and colors related to this event will be written
     * @param tagColors tag color combinations that'll be written
     */
    public static void writeTagColorsToFile(Long eventId, Map<String, String> tagColors) {
        File file = new File(TAG_COLORS_DIRECTORY + eventId + TAG_COLORS_FILE_EXTENSION);
        File directory = new File(TAG_COLORS_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(file, false))) {
            for (Map.Entry<String, String> entry : tagColors.entrySet()) {
                out.println(entry.getKey() + "=" + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Writing to file wasn't successful: " + e.getMessage());
        }
    }
}

