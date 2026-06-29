package com.company.knowledge.repository;

import com.company.knowledge.entity.CustomerCompany;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Long>, JpaSpecificationExecutor<CustomerCompany> {
    List<CustomerCompany> findAllByOrderByUpdatedAtDesc();
}
