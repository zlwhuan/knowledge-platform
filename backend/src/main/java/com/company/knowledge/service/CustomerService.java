package com.company.knowledge.service;

import com.company.knowledge.dto.CustomerCompanyRequest;
import com.company.knowledge.dto.CustomerCompanyResponse;
import com.company.knowledge.dto.CustomerContactRequest;
import com.company.knowledge.dto.CustomerContactResponse;
import com.company.knowledge.dto.CustomerFollowupRequest;
import com.company.knowledge.dto.CustomerFollowupResponse;
import java.util.List;

public interface CustomerService {
    List<CustomerCompanyResponse> list(String keyword, String ownerName, String status, String level, String region);
    CustomerCompanyResponse get(Long id);
    CustomerCompanyResponse create(CustomerCompanyRequest request, String operatorName);
    CustomerCompanyResponse update(Long id, CustomerCompanyRequest request, String operatorName);
    void delete(Long id);
    CustomerContactResponse addContact(Long customerId, CustomerContactRequest request, String operatorName);
    CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request, String operatorName);
    void deleteContact(Long contactId);
    CustomerFollowupResponse addFollowup(Long customerId, CustomerFollowupRequest request, String operatorName);
    CustomerFollowupResponse updateFollowup(Long followupId, CustomerFollowupRequest request, String operatorName);
    void deleteFollowup(Long followupId);
}
