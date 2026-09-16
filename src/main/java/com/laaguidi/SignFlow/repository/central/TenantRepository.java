package com.laaguidi.SignFlow.repository.central;

import com.laaguidi.SignFlow.model.central.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantRepository  extends JpaRepository<Tenant, UUID> {

    @Query("SELECT t FROM Tenant t WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            " LOWER(t.dbSchema) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(t.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(t.adminEmail) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Tenant> searchTenants(@Param("query") String query, Pageable pageable);

    boolean existsByDbSchema(String dbSchema);
}
