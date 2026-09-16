package com.laaguidi.SignFlow.service.central;

import com.laaguidi.SignFlow.exception.TenantNotFoundException;
import com.laaguidi.SignFlow.model.central.dto.request.TenantStatusRequest;
import com.laaguidi.SignFlow.model.central.entity.Tenant;
import com.laaguidi.SignFlow.model.dto.request.TenantRequest;
import com.laaguidi.SignFlow.model.dto.response.TenantResponse;
import com.laaguidi.SignFlow.model.enums.PermissionEnum;
import com.laaguidi.SignFlow.model.enums.Status;
import com.laaguidi.SignFlow.model.mapper.central.TenantMapper;
import com.laaguidi.SignFlow.repository.central.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.springframework.data.domain.*;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final TenantMapper tenantMapper;
    private final CentralMonitoringService monitoringService;
    private final HttpServletRequest httpRequest;

    /**
     * Non-transactional root method to safely handle dynamic schema DDL and programmatic seeding.
     */
    public TenantResponse createTenantWithAdmin(TenantRequest request) {
        String schemaName = buildSchemaName(request.getDbSchema());

        if (tenantRepository.existsByDbSchema(schemaName)) {
            throw new RuntimeException("Tenant Schema already exists: " + schemaName);
        }

        Tenant tenant = null;
        try {
            // 1. Create Schema (fails loudly on collision instead of silently reusing it)
            createTenantSchema(schemaName);

            // 2. Execute Hibernate schema DDL
            runHibernateMigrations(schemaName);

            // 3. Seed roles, permissions & admin user (atomic - all or nothing)
            seedTenantAdminData(schemaName, request);

            // 4. Save metadata to Central DB in an isolated transaction
            tenant = saveTenantMetadata(schemaName, request);

            // 5. Audit - best effort only. A failure here must NOT trigger a schema
            // rollback, because the tenant row above already committed independently
            // (REQUIRES_NEW). Rolling back the schema after that would orphan the
            // central DB record.
            try {
                monitoringService.logEvent(
                        "TENANT_CREATED",
                        getAuthenticatedAdminEmail(),
                        "tenant:" + tenant.getId().toString(),
                        "Created tenant '" + request.getCompanyName() + "' with admin email: " + request.getAdminEmail(),
                        getClientIpAddress()
                );
            } catch (Exception auditEx) {
                log.error("Tenant {} was created successfully but audit logging failed", tenant.getId(), auditEx);
            }

            return tenantMapper.tenantToTenantResponse(tenant);

        } catch (Exception e) {
            log.error("Failed provisioning tenant {}. Rolling back...", schemaName, e);
            if (tenant != null) {
                // Metadata was already committed (REQUIRES_NEW) - clean it up too,
                // otherwise we'd be left with a Tenant row pointing at a dropped schema.
                try {
                    tenantRepository.delete(tenant);
                } catch (Exception cleanupEx) {
                    log.error("Failed to clean up orphaned tenant metadata for schema {}", schemaName, cleanupEx);
                }
            }
            rollbackSchema(schemaName);
            throw new RuntimeException("Tenant provisioning failed: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tenant saveTenantMetadata(String schemaName, TenantRequest request) {
        Tenant tenant = Tenant.builder()
                .companyName(request.getCompanyName())
                .dbSchema(schemaName)
                .adminEmail(request.getAdminEmail())
                .adminPassword(passwordEncoder.encode(request.getAdminPassword()))
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Tenant savedTenant = tenantRepository.saveAndFlush(tenant);
        log.info("Tenant successfully saved in central DB: {} with Schema: {}", savedTenant.getId(), schemaName);
        return savedTenant;
    }

    @Transactional(readOnly = true)
    public PagedModel<TenantResponse> getAllTenants(String query, Pageable pageable) {
        Page<Tenant> tenantsPage;

        if (StringUtils.hasText(query)) {
            tenantsPage = tenantRepository.searchTenants(query.trim(), pageable);
        } else {
            tenantsPage = tenantRepository.findAll(pageable);
        }

        // Map entities to DTOs
        List<TenantResponse> tenantResponses = tenantMapper.tenantToTenantResponse(tenantsPage.getContent());

        Page<TenantResponse> responsePage = new PageImpl<>(
                tenantResponses,
                tenantsPage.getPageable(),
                tenantsPage.getTotalElements()
        );

        return new PagedModel<>(responsePage);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with ID: " + tenantId));
        return tenantMapper.tenantToTenantResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenantStatus(UUID tenantId, TenantStatusRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with ID: " + tenantId));

        Status oldStatus = tenant.getStatus();
        tenant.setStatus(request.getStatus());
        Tenant updatedTenant = tenantRepository.save(tenant);

        log.info("Tenant {} status updated to: {}", tenantId, updatedTenant.getStatus());

        monitoringService.logEvent(
                "TENANT_STATUS_UPDATED",
                getAuthenticatedAdminEmail(),
                "tenant:" + tenantId,
                "Changed tenant status from " + oldStatus + " to " + updatedTenant.getStatus(),
                getClientIpAddress()
        );

        return tenantMapper.tenantToTenantResponse(updatedTenant);
    }

    @Transactional
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with ID: " + tenantId));

        String schemaName = tenant.getDbSchema();

        rollbackSchema(schemaName);

        tenantRepository.delete(tenant);
        log.info("Tenant deleted successfully from central database: {}", tenantId);

        monitoringService.logEvent(
                "TENANT_DELETED",
                getAuthenticatedAdminEmail(),
                "tenant:" + tenantId,
                "Deleted tenant and dropped schema: " + schemaName,
                getClientIpAddress()
        );
    }

    /**
     * Sanitizes and validates the raw schema name supplied by the caller.
     * Throws early (before any DB work) if the result would be null, blank,
     * or degenerate (e.g. an input made entirely of invalid characters).
     */
    private String buildSchemaName(String rawDbSchema) {
        if (!StringUtils.hasText(rawDbSchema)) {
            throw new IllegalArgumentException("dbSchema is required");
        }

        String sanitized = rawDbSchema.toLowerCase().replaceAll("[^a-z0-9_]", "");

        if (!StringUtils.hasText(sanitized)) {
            throw new IllegalArgumentException(
                    "dbSchema must contain at least one alphanumeric character");
        }

        return "tenant_" + sanitized;
    }

    private void createTenantSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Intentionally no "IF NOT EXISTS": if the schema already exists
            // (e.g. a concurrent request raced past the existsByDbSchema check),
            // this must fail loudly rather than silently reusing another
            // tenant's schema. A unique DB constraint on db_schema is the
            // authoritative guard; this is defense in depth.
            String sql = "CREATE SCHEMA \"" + schemaName + "\"";
            statement.execute(sql);

            log.info("Created DB Schema: {}", schemaName);

        } catch (Exception e) {
            log.error("Error creating schema {}", schemaName, e);
            throw new RuntimeException("Could not create database schema for tenant", e);
        }
    }

    private void runHibernateMigrations(String schemaName) {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName());
            settings.put(AvailableSettings.DEFAULT_SCHEMA, schemaName);
            settings.put(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create");
            settings.put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
            settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(settings)
                    .build();

            try {
                MetadataSources metadataSources = new MetadataSources(registry);

                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.User.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.Role.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.Permission.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.AuditLog.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.Document.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.DocumentSigner.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.RefreshToken.class);
                metadataSources.addAnnotatedClass(com.laaguidi.SignFlow.model.entity.SignatureField.class);

                Metadata metadata = metadataSources.getMetadataBuilder().build();

                SchemaManagementToolCoordinator.process(
                        metadata,
                        registry,
                        settings,
                        action -> {}
                );

                log.info("Successfully generated schema tables for: {}", schemaName);

            } finally {
                StandardServiceRegistryBuilder.destroy(registry);
            }

        } catch (Exception e) {
            log.error("Failed to execute Hibernate schema generation for schema {}", schemaName, e);
            throw new RuntimeException("Could not run database migrations for tenant", e);
        }
    }

    private void seedTenantAdminData(String schemaName, TenantRequest request) {
        String insertPermissionSql = "INSERT INTO permissions (id, name, description) VALUES (?, ?, ?) ON CONFLICT (name) DO NOTHING;";
        String insertRoleSql = "INSERT INTO roles (id, name) VALUES (?, ?) ON CONFLICT (name) DO NOTHING;";
        String mapRolePermissionSql = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?);";
        String insertUserSql = "INSERT INTO users (id, full_name, email, password, role_id) VALUES (?, ?, ?, ?, ?);";

        try (Connection connection = dataSource.getConnection()) {
            // Run the whole seed sequence as a single transaction: either every
            // permission/role/mapping/user insert succeeds, or none of them do.
            // Previously each executeBatch() auto-committed independently, which
            // could leave roles/permissions seeded but no admin user created.
            connection.setAutoCommit(false);

            try {
                connection.setSchema(schemaName);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET search_path TO \"" + schemaName + "\"");
                }

                Map<PermissionEnum, UUID> permissionMap = new HashMap<>();

                // 1. Insert Permissions
                try (PreparedStatement pstmtPerm = connection.prepareStatement(insertPermissionSql)) {
                    for (PermissionEnum perm : PermissionEnum.values()) {
                        UUID permId = UUID.randomUUID();
                        permissionMap.put(perm, permId);

                        pstmtPerm.setObject(1, permId);
                        pstmtPerm.setString(2, perm.name());
                        pstmtPerm.setString(3, perm.getDescription());
                        pstmtPerm.addBatch();
                    }
                    pstmtPerm.executeBatch();
                }

                // 2. Create Roles
                UUID adminRoleId = UUID.randomUUID();
                UUID userRoleId = UUID.randomUUID();

                try (PreparedStatement pstmtRole = connection.prepareStatement(insertRoleSql)) {
                    pstmtRole.setObject(1, adminRoleId);
                    pstmtRole.setString(2, "TENANT_ADMIN");
                    pstmtRole.addBatch();

                    pstmtRole.setObject(1, userRoleId);
                    pstmtRole.setString(2, "USER");
                    pstmtRole.addBatch();

                    pstmtRole.executeBatch();
                }

                // 3. Map Permissions to TENANT_ADMIN
                try (PreparedStatement pstmtMap = connection.prepareStatement(mapRolePermissionSql)) {
                    for (UUID permId : permissionMap.values()) {
                        pstmtMap.setObject(1, adminRoleId);
                        pstmtMap.setObject(2, permId);
                        pstmtMap.addBatch();
                    }
                    pstmtMap.executeBatch();
                }

                // 4. Create Tenant Admin User
                try (PreparedStatement pstmtUser = connection.prepareStatement(insertUserSql)) {
                    String encodedPassword = passwordEncoder.encode(request.getAdminPassword());

                    pstmtUser.setObject(1, UUID.randomUUID());
                    pstmtUser.setString(2, request.getAdminFullName());
                    pstmtUser.setString(3, request.getAdminEmail());
                    pstmtUser.setString(4, encodedPassword);
                    pstmtUser.setObject(5, adminRoleId);

                    pstmtUser.executeUpdate();
                }

                connection.commit();
                log.info("Tenant Admin, Roles, and Permissions seeded successfully in schema: {}", schemaName);

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }

        } catch (Exception e) {
            log.error("Error seeding tenant data in schema {}", schemaName, e);
            throw new RuntimeException("Could not seed tenant initialization data", e);
        }
    }

    private void rollbackSchema(String schemaName) {
        if (schemaName != null && !schemaName.isEmpty()) {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                String dropSchemaQuery = "DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE";
                statement.execute(dropSchemaQuery);
                log.info("Successfully dropped database schema: {}", schemaName);
            } catch (Exception e) {
                log.error("Failed to drop tenant schema {}: {}", schemaName, e.getMessage());
            }
        }
    }

    private String getAuthenticatedAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String getClientIpAddress() {
        if (httpRequest == null) return "UNKNOWN";
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }
}