package com.laaguidi.SignFlow.repository;

import com.laaguidi.SignFlow.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface TokenRepository extends JpaRepository<RefreshToken, UUID> {
}
