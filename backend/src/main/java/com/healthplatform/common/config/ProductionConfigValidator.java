package com.healthplatform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start in the prod profile with a known-weak or placeholder configuration, so a
 * mis-deployed environment fails loudly at boot instead of running with a guessable JWT key.
 */
@Component
@Profile("prod")
public class ProductionConfigValidator {

    private final String jwtSecret;
    private final String dbPassword;
    private final String corsOrigins;

    public ProductionConfigValidator(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${app.cors.allowed-origins}") String corsOrigins
    ) {
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
        this.corsOrigins = corsOrigins;
    }

    @PostConstruct
    void validate() {
        List<String> problems = check(jwtSecret, dbPassword, corsOrigins);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start with insecure production configuration: " + String.join("; ", problems));
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
