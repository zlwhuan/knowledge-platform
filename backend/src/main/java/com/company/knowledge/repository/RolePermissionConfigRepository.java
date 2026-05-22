package com.company.knowledge.repository;

import com.company.knowledge.entity.RolePermissionConfig;
import com.company.knowledge.entity.RoleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionConfigRepository extends JpaRepository<RolePermissionConfig, Long> {
    Optional<RolePermissionConfig> findByRole(RoleType role);
}
