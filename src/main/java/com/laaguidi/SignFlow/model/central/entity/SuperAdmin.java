package com.laaguidi.SignFlow.model.central.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "super_admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdmin implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = "SUPER_ADMIN";
        }
    }

    // ==================== IMPLEMENTATION DE USERDETAILS ====================

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = role;
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public @NonNull String getUsername() {
        return this.email;
    }


}