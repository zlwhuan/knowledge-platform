package com.company.knowledge.repository;

import com.company.knowledge.entity.CustomerContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {
    List<CustomerContact> findAllByCustomerIdOrderByPrimaryContactDescUpdatedAtDesc(Long customerId);
    void deleteAllByCustomerId(Long customerId);
}
