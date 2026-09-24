package com.healthplatform;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The unit/integration suites run on H2 with Flyway disabled, so the real PostgreSQL migrations are
 * never exercised there. This applies every production migration to a real Postgres 16 and checks the
 * things H2 cannot: the schema builds, the audit table is really append-only, and the production
 * migration path contains no dev seed accounts.
 *
 * Skipped automatically when Docker is not available (runs in CI).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static Flyway flyway(String... locations) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(locations)
                .cleanDisabled(false)
                .load();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @Test
    void productionMigrationsApplyCleanlyAndSeedNoAccounts() throws Exception {
        Flyway flyway = flyway("classpath:db/migration");
        flyway.clean();
        flyway.migrate();

        try (Connection c = connect(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("select count(*) from users");
            rs.next();
            assertEquals(0, rs.getInt(1), "production migrations must not create any user account");
        }
    }

    @Test
    void devSeedIsOnlyAppliedWhenTheDevLocationIsIncluded() throws Exception {
        Flyway flyway = flyway("classpath:db/migration", "classpath:db/dev");
        flyway.clean();
        flyway.migrate();

        try (Connection c = connect(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("select count(*) from users where email = 'admin@hospital.test'");
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void auditLogRejectsUpdateAndDelete() throws Exception {
        Flyway flyway = flyway("classpath:db/migration");
        flyway.clean();
        flyway.migrate();

        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("insert into audit_log (action, outcome) values ('READ', 'SUCCESS')");
            assertThrows(SQLException.class, () -> s.executeUpdate("update audit_log set outcome = 'FAILURE'"));
            assertThrows(SQLException.class, () -> s.executeUpdate("delete from audit_log"));
            ResultSet rs = s.executeQuery("select count(*) from audit_log");
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}
