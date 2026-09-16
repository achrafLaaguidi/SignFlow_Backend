package com.laaguidi.SignFlow.repository.central;

import com.laaguidi.SignFlow.model.central.entity.CentralAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CentralAuditLogRepository extends JpaRepository<CentralAuditLog, UUID> {

    @Query("SELECT a FROM CentralAuditLog a WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.targetResource) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<CentralAuditLog> searchAuditLogs(@Param("query") String query, Pageable pageable);

}