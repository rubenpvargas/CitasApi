package com.fcv.citas.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regla de arquitectura (escaneo de fuentes): el dominio y la aplicación no dependen de Spring, JDBC ni
 * JPA; los controladores REST no devuelven {@code Map<String,Object>}.
 */
class HexagonalArchitectureTest {
    private static final Path ROOT = Path.of("src", "main", "java", "com", "fcv", "citas");

    @Test
    void applicationLayerHasNoJdbcSqlOrSpringDependencies() throws IOException {
        assertThat(violations(ROOT.resolve("application"),
                Pattern.compile("^import\\s+(org\\.springframework\\.|java\\.sql\\.|jakarta\\.persistence\\.)")))
                .as("application must depend only on ports").isEmpty();
    }

    @Test
    void domainLayerHasNoFrameworkDependencies() throws IOException {
        assertThat(violations(ROOT.resolve("domain"),
                Pattern.compile("^import\\s+(org\\.springframework\\.|jakarta\\.|java\\.sql\\.|com\\.fasterxml\\.)")))
                .as("domain must be framework-free").isEmpty();
    }

    @Test
    void restAdaptersExposeTypedResponsesOnly() throws IOException {
        assertThat(violations(ROOT.resolve("adapter").resolve("in").resolve("rest"),
                Pattern.compile("Map<String,\\s*Object>")))
                .as("REST responses must be typed records").isEmpty();
    }

    @Test
    void jdbcTemplateLivesOnlyInPersistenceAdapters() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                boolean persistence = file.startsWith(ROOT.resolve("adapter").resolve("out").resolve("persistence"));
                if (!persistence && Files.readString(file, StandardCharsets.UTF_8).contains("JdbcTemplate")) {
                    offenders.add(ROOT.relativize(file).toString());
                }
            }
        }
        assertThat(offenders).isEmpty();
    }

    private static List<String> violations(Path dir, Pattern pattern) throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (pattern.matcher(line.trim()).find()) {
                        found.add(ROOT.relativize(file) + ": " + line.trim());
                    }
                }
            }
        }
        return found;
    }
}
