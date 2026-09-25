package com.healthplatform;

import com.healthplatform.common.config.ProductionConfigValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // expired refresh-token purge (RefreshTokenService)
public class HealthcarePlatformApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HealthcarePlatformApplication.class);
        // Fail fast on insecure prod configuration before any bean (DB, Flyway, web server) starts.
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
                ProductionConfigValidator.validate(event.getEnvironment()));
        app.run(args);
    }
}
