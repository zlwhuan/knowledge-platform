package com.company.knowledge.repository;

import com.company.knowledge.entity.CustomerFollowup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerFollowupRepository extends JpaRepository<CustomerFollowup, Long> {
    List<CustomerFollowup> findAllByCustomerIdOrderByFollowupTimeDesc(Long customerId);
    void deleteAllByCustomerId(Long customerId);
}
