package client.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.io.File;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class TagColorManagerUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    public void testReadTagColorsFromFileExists() {
        // Setup
        Long eventId = 1L;
        File testFile = tempDir.resolve("1tagColors.txt").toFile();
        try (PrintWriter out = new PrintWriter(testFile)) {
            out.println("food=red");
            out.println("transport=blue");
        } catch (Exception e) {
            throw new RuntimeException();
        }

        TagColorManagerUtils.TAG_COLORS_DIRECTORY = tempDir.toString() + File.separator;
        Map<String, String> colors = TagColorManagerUtils.readTagColorsFromFile(eventId);

        assertNotNull(colors);
        assertEquals("red", colors.get("food"));
        assertEquals("blue", colors.get("transport"));
    }

    @Test
    public void testReadTagColorsFromFileNotExists() {
        TagColorManagerUtils.TAG_COLORS_DIRECTORY = tempDir.toString() + File.separator;
        assertTrue(TagColorManagerUtils.readTagColorsFromFile(2L).isEmpty());
    }

    @Test
    public void testWriteTagColorsToFile() {
        Long eventId = 3L;
        Map<String, String> tagColors = Map.of("food", "red", "transport", "blue");

        TagColorManagerUtils.TAG_COLORS_DIRECTORY = tempDir.toString() + File.separator;
        TagColorManagerUtils.writeTagColorsToFile(eventId, tagColors);

        File writtenFile = tempDir.resolve("3tagColors.txt").toFile();
        assertTrue(writtenFile.exists());
        try (Scanner scanner = new Scanner(writtenFile)) {
            for (int i = 0; i < 2; i++) {
                String line = scanner.nextLine();
                if (!("food=red".equals(line) || "transport=blue".equals(line))) {
                    fail(line + "\nWas written to file");
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
    }
}