package server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/api/admin")
public class PasswordController {
    private static String password;

    /**
     * Generate a random password for the admin
     */
    public static String generatePassword() {
        Random random = new Random();
        StringBuilder strBuilder = new StringBuilder();
        int length = random.nextInt(20, 25);
        for (int i = 0; i < length; i++) {
            int j = random.nextInt(26 + 26 + 10);
            if (j < 26) {
                strBuilder.append((char)('a' + j));
            } else if (j < 26 * 2) {
                strBuilder.append((char)('A' + j - 26));
            } else {
                strBuilder.append((char)('0' + j - 2 * 26));
            }
        }
        password = strBuilder.toString();
        return password;
    }

    @PostMapping
    public ResponseEntity<Boolean> checkPassword(@RequestBody String attempt) {
        return ResponseEntity.ok(password.equals(attempt));
    }
}
