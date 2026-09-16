package com.laaguidi.SignFlow.config;

import com.laaguidi.SignFlow.repository.UserRepository;
import com.laaguidi.SignFlow.repository.central.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    @Bean
    public UserDetailsService tenantUserDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found in tenant DB"));
    }

    @Bean
    public UserDetailsService superAdminUserDetailsService(SuperAdminRepository superAdminRepository) {
        return username -> superAdminRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("SuperAdmin not found in Central DB"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            @Qualifier("tenantUserDetailsService") UserDetailsService tenantUserDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(tenantUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}