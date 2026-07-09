package com.suvidha.auth.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationValidationTest {

    private static final Pattern MIGRATION_FILE_PATTERN =
            Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    @Test
    void allMigrationsAreSequentiallyNumbered() throws Exception {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");

        assertTrue(resources.length > 0, "No migration files found");

        int[] versions = Stream.of(resources)
                .map(r -> {
                    var matcher = MIGRATION_FILE_PATTERN.matcher(r.getFilename());
                    assertTrue(matcher.matches(), "Invalid migration filename: " + r.getFilename());
                    return Integer.parseInt(matcher.group(1));
                })
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();

        for (int i = 0; i < versions.length; i++) {
            assertEquals(i + 1, versions[i],
                    "Migration versions must be sequential starting from 1. Found gap at version " + versions[i]);
        }
    }

    @Test
    void allMigrationFilesAreReadable() throws Exception {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");

        for (Resource r : resources) {
            try (InputStream is = r.getInputStream()) {
                byte[] content = is.readAllBytes();
                assertTrue(content.length > 0, "Migration file is empty: " + r.getFilename());
                String sql = new String(content, StandardCharsets.UTF_8);
                assertFalse(sql.isBlank(), "Migration file contains only whitespace: " + r.getFilename());
            }
        }
    }
}
