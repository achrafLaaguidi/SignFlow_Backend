package com.laaguidi.SignFlow.repository.central;

import com.laaguidi.SignFlow.model.central.entity.CentralRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CentralRefreshTokenRepository extends JpaRepository<CentralRefreshToken, UUID> {
    Optional<CentralRefreshToken> findByToken(String token);
    @Query("""
        SELECT t FROM CentralRefreshToken t 
        WHERE t.superAdmin.id = :superAdminId 
          AND t.revoked = false 
          AND t.expiryDate > CURRENT_TIMESTAMP
    """)
    List<CentralRefreshToken> findAllValidTokensBySuperAdmin(@Param("superAdminId") UUID superAdminId);
}
