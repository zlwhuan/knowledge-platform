package com.company.knowledge.repository;

import com.company.knowledge.entity.CustomerCompany;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Long> {
    List<CustomerCompany> findAllByOrderByUpdatedAtDesc();
}
