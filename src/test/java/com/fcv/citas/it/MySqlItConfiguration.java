package com.fcv.citas.it;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deja la base de pruebas vacía y la migra con Flyway la primera vez que arranca un contexto en la JVM.
 * Contextos posteriores (p. ej. con otras propiedades) solo migran, para no borrar datos en uso.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlItConfiguration {
    private static final AtomicBoolean CLEANED = new AtomicBoolean(false);

    @Bean
    FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            if (CLEANED.compareAndSet(false, true)) {
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
