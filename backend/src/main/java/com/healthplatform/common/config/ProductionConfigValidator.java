package com.healthplatform.common.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start in the prod profile with a known-weak or placeholder configuration, so a
 * mis-deployed environment fails loudly at boot instead of running with a guessable JWT key.
 *
 * Invoked from {@link com.healthplatform.HealthcarePlatformApplication#main} as soon as the
 * environment is prepared — BEFORE any bean is created — so a bad configuration can never get as far
 * as connecting to (or running Flyway migrations against) the production database.
 */
public final class ProductionConfigValidator {

    private ProductionConfigValidator() {}

    /** No-op unless the prod profile is active. Throws IllegalStateException listing every problem. */
    public static void validate(Environment env) {
        if (!env.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        List<String> problems = check(
                property(env, "app.jwt.secret"),
                property(env, "spring.datasource.password"),
                property(env, "app.cors.allowed-origins"));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start with insecure production configuration: " + String.join("; ", problems));
        }
    }

    // A required ${PLACEHOLDER} with no value makes getProperty throw; treat "unresolvable" as "missing".
    private static String property(Environment env, String key) {
        try {
            return env.getProperty(key);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return null;
        }
    }

    static List<String> check(String jwtSecret, String dbPassword, String corsOrigins) {
        List<String> problems = new ArrayList<>();
        if (jwtSecret == null || jwtSecret.length() < 32 || jwtSecret.toUpperCase().contains("CHANGE_ME") || jwtSecret.toUpperCase().contains("REPLACE_ME")) {
            problems.add("JWT_SECRET must be a random value of at least 32 characters (not a placeholder)");
        }
        if (dbPassword == null || dbPassword.length() < 12 || dbPassword.equalsIgnoreCase("changeme") || dbPassword.toUpperCase().contains("REPLACE_ME")) {
            problems.add("DB_PASSWORD must be a strong value of at least 12 characters (not a placeholder)");
        }
        if (corsOrigins == null || corsOrigins.isBlank() || corsOrigins.contains("*") || corsOrigins.contains("localhost")) {
            problems.add("CORS_ALLOWED_ORIGINS must list the real https origin(s) — no wildcard or localhost");
        }
        return problems;
    }
}
