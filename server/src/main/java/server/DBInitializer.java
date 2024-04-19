package server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DBInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DBInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if the EVENTS table is empty
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM EVENTS", Integer.class);
        if (count != null && count == 0) {
            ClassPathResource resource = new ClassPathResource("data.sql");
            String sql = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            jdbcTemplate.execute(sql);
        }
    }
}
