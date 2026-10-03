package com.fcv.citas.it;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Evidencia de que MySQL parte de una base vacía y Flyway la deja lista (V1..Vn aplicadas con éxito). */
class FlywayEmptyDatabaseIT extends AbstractMySqlIT {
    @Test
    void flywayAppliesEveryMigrationSuccessfullyFromAnEmptySchema() {
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE", Integer.class);
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL AND success = TRUE", Integer.class);
        Integer firstRank = jdbc.queryForObject(
                "SELECT MIN(installed_rank) FROM flyway_schema_history", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(8);
        assertThat(firstRank).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE code IN ('USER','PROFESSIONAL','ADMIN')",
                Integer.class)).isEqualTo(3);
    }
}
