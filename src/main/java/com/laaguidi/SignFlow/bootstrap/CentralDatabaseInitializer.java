package com.laaguidi.SignFlow.bootstrap;

import com.laaguidi.SignFlow.model.central.entity.CentralAuditLog;
import com.laaguidi.SignFlow.model.central.entity.CentralRefreshToken;
import com.laaguidi.SignFlow.model.central.entity.SuperAdmin;
import com.laaguidi.SignFlow.model.central.entity.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.jspecify.annotations.NonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CentralDatabaseInitializer {

    private static final String HOST = getEnvOrDefault("DB_HOST", "localhost");
    private static final String PORT = getEnvOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = getEnvOrDefault("DB_NAME", "central");
    private static final String USER = getEnvOrDefault("DB_USERNAME", "root"); // PostgreSQL Default
    private static final String PASSWORD = getEnvOrDefault("DB_PASSWORD", "root");
    private static final String SSL_MODE = getEnvOrDefault("DB_SSL_MODE", "disable");

    // 1️⃣ الاتصال بـ postgres DB الافتراضية باش نتحققوا ونكرييو central DB
    private static final String DEFAULT_DB_URL = String.format("jdbc:postgresql://%s:%s/postgres?sslmode=%s", HOST, PORT, SSL_MODE);

    // 2️⃣ الاتصال بـ central DB باش نكرييو الجداول
    private static final String TARGET_DB_URL = String.format("jdbc:postgresql://%s:%s/%s?sslmode=%s", HOST, PORT, DB_NAME, SSL_MODE);

    public static void createDatabaseAndTablesIfNotExists() {
        createDatabaseIfNotExists();
        createCentralTablesOnly();
    }

    private static void createDatabaseIfNotExists() {
        try (Connection connection = DriverManager.getConnection(DEFAULT_DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'");
            if (!resultSet.next()) {
                statement.executeUpdate("CREATE DATABASE " + DB_NAME);
                log.info("Central Database '{}' created successfully.", DB_NAME);
            } else {
                log.info("Central Database '{}' already exists.", DB_NAME);
            }
        } catch (Exception e) {
            log.error("Error creating central database '{}': {}", DB_NAME, e.getMessage(), e);
        }
    }

    private static void createCentralTablesOnly() {
        ServiceRegistry registry = null;
        try {
            Map<String, Object> settings = getStringObjectMap();

            var registryBuilder = new StandardServiceRegistryBuilder();
            registryBuilder.applySettings(settings);
            registry = registryBuilder.build();

            MetadataSources metadataSources = new MetadataSources(registry);

            metadataSources.addAnnotatedClass(SuperAdmin.class);
            metadataSources.addAnnotatedClass(Tenant.class);
            metadataSources.addAnnotatedClass(CentralRefreshToken.class);
            metadataSources.addAnnotatedClass(CentralAuditLog.class);

            var metadataBuilder = metadataSources.getMetadataBuilder();
            var metadata = metadataBuilder.build();

            SchemaManagementToolCoordinator.process(
                    metadata,
                    registry,
                    settings,
                    action -> {}
            );

            log.info("Central schema tables created successfully!");
        } catch (Exception e) {
            log.error("Error initializing central schema tables: {}", e.getMessage(), e);
        } finally {
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
        }
    }

    private static @NonNull Map<String, Object> getStringObjectMap() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", TARGET_DB_URL);
        settings.put("hibernate.connection.username", USER);
        settings.put("hibernate.connection.password", PASSWORD);
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "update");
        settings.put("hibernate.show_sql", "true");
        settings.put("hibernate.format_sql", "true");
        settings.put("hibernate.schema_management_tool", "org.hibernate.tool.schema.internal.HibernateSchemaManagementTool");
        return settings;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}