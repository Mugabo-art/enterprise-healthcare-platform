package com.healthplatform.common.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigValidatorTest {

    private static final String GOOD_SECRET = "k3Jx9vQ2mZp7Lw0tRb5NcYd8HsGf1AuE4iOo6TyU";
    private static final String GOOD_DB = "correct-horse-battery";
    private static final String GOOD_CORS = "https://healthcare.example.com";

    @SuppressWarnings("unchecked")
    private static List<String> check(String jwt, String db, String cors) throws Exception {
        Method m = ProductionConfigValidator.class.getDeclaredMethod("check", String.class, String.class, String.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, jwt, db, cors);
    }

    @Test
    void acceptsStrongConfiguration() throws Exception {
        assertTrue(check(GOOD_SECRET, GOOD_DB, GOOD_CORS).isEmpty());
    }

    @Test
    void rejectsPlaceholderJwtSecret() throws Exception {
        assertEquals(1, check("CHANGE_ME_IN_PRODUCTION_this_must_be_at_least_32_bytes_long", GOOD_DB, GOOD_CORS).size());
    }

    @Test
    void rejectsShortJwtSecret() throws Exception {
        assertEquals(1, check("short", GOOD_DB, GOOD_CORS).size());
    }

    @Test
    void rejectsDefaultDbPassword() throws Exception {
        assertEquals(1, check(GOOD_SECRET, "changeme", GOOD_CORS).size());
    }

    @Test
    void rejectsWildcardOrLocalhostCors() throws Exception {
        assertEquals(1, check(GOOD_SECRET, GOOD_DB, "*").size());
        assertEquals(1, check(GOOD_SECRET, GOOD_DB, "http://localhost:5173").size());
        assertEquals(1, check(GOOD_SECRET, GOOD_DB, "").size());
    }

    @Test
    void reportsEveryProblemAtOnce() throws Exception {
        assertEquals(3, check("x", "x", "*").size());
    }
}
