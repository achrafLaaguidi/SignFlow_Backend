package com.laaguidi.SignFlow.bootstrap;

import com.laaguidi.SignFlow.config.tenant.TenantContext;
import com.laaguidi.SignFlow.model.central.entity.SuperAdmin;
import com.laaguidi.SignFlow.repository.central.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CentralDataSeeder implements ApplicationRunner {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.central.super-admin.email}")
    private String defaultAdminEmail;

    @Value("${application.central.super-admin.password}")
    private String defaultAdminPassword;

    @Value("${application.central.super-admin.name}")
    private String defaultAdminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
             TenantContext.setCurrentTenant("public");

            seedInitialSuperAdmin();
        } catch (Exception e) {
            log.warn("Could not seed SuperAdmin. Reason: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private void seedInitialSuperAdmin() {
        if (!superAdminRepository.existsByEmail(defaultAdminEmail)) {
            log.info("No SuperAdmin found in Central DB. Initializing root SuperAdmin account...");

            SuperAdmin rootAdmin = SuperAdmin.builder()
                    .fullName(defaultAdminName)
                    .email(defaultAdminEmail)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .role("SUPER_ADMIN")
                    .build();

            superAdminRepository.save(rootAdmin);

            log.info("Root SuperAdmin account created successfully with email: {}", defaultAdminEmail);
        } else {
            log.info("SuperAdmin account ('{}') already exists. Skipping Central DB seeding.", defaultAdminEmail);
        }
    }
}