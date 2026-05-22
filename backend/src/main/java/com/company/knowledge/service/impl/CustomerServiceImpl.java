package com.company.knowledge.service.impl;

import com.company.knowledge.dto.CustomerCompanyRequest;
import com.company.knowledge.dto.CustomerCompanyResponse;
import com.company.knowledge.dto.CustomerContactRequest;
import com.company.knowledge.dto.CustomerContactResponse;
import com.company.knowledge.dto.CustomerFollowupRequest;
import com.company.knowledge.dto.CustomerFollowupResponse;
import com.company.knowledge.entity.CustomerCompany;
import com.company.knowledge.entity.CustomerContact;
import com.company.knowledge.entity.CustomerFollowup;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.CustomerCompanyRepository;
import com.company.knowledge.repository.CustomerContactRepository;
import com.company.knowledge.repository.CustomerFollowupRepository;
import com.company.knowledge.service.CustomerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerContactRepository customerContactRepository;
    private final CustomerFollowupRepository customerFollowupRepository;

    public CustomerServiceImpl(CustomerCompanyRepository customerCompanyRepository, CustomerContactRepository customerContactRepository, CustomerFollowupRepository customerFollowupRepository) {
        this.customerCompanyRepository = customerCompanyRepository;
        this.customerContactRepository = customerContactRepository;
        this.customerFollowupRepository = customerFollowupRepository;
    }

    @Override
    public List<CustomerCompanyResponse> list(String keyword, String ownerName, String status, String level, String region) {
        return customerCompanyRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(company -> matches(company.getName(), keyword)
                        || matches(company.getShortName(), keyword)
                        || matches(company.getIndustry(), keyword)
                        || matches(company.getTags(), keyword)
                        || matches(company.getNotes(), keyword))
                .filter(company -> matches(company.getOwnerName(), ownerName))
                .filter(company -> equalsValue(company.getStatus(), status))
                .filter(company -> equalsValue(company.getLevel(), level))
                .filter(company -> equalsValue(company.getRegion(), region))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CustomerCompanyResponse get(Long id) {
        return toResponse(findCompany(id));
    }

    @Override
    public CustomerCompanyResponse create(CustomerCompanyRequest request, String operatorName) {
        CustomerCompany company = new CustomerCompany();
        applyCompany(company, request, operatorName);
        if (!StringUtils.hasText(company.getCreatedBy())) {
            company.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
        return toResponse(customerCompanyRepository.save(company));
    }

    @Override
    public CustomerCompanyResponse update(Long id, CustomerCompanyRequest request, String operatorName) {
        CustomerCompany company = findCompany(id);
        applyCompany(company, request, operatorName);
        return toResponse(customerCompanyRepository.save(company));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CustomerCompany company = findCompany(id);
        customerContactRepository.deleteAllByCustomerId(id);
        customerFollowupRepository.deleteAllByCustomerId(id);
        customerCompanyRepository.delete(company);
    }

    @Override
    public CustomerContactResponse addContact(Long customerId, CustomerContactRequest request, String operatorName) {
        CustomerCompany company = findCompany(customerId);
        CustomerContact contact = new CustomerContact();
        contact.setCustomer(company);
        applyContact(contact, request, operatorName, false);
        CustomerContact saved = customerContactRepository.save(contact);
        touchCompany(company, operatorName);
        return toContactResponse(saved);
    }

    @Override
    public CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request, String operatorName) {
        CustomerContact contact = findContact(contactId);
        applyContact(contact, request, operatorName, true);
        CustomerContact saved = customerContactRepository.save(contact);
        touchCompany(saved.getCustomer(), operatorName);
        return toContactResponse(saved);
    }

    @Override
    public void deleteContact(Long contactId) {
        CustomerContact contact = findContact(contactId);
        CustomerCompany company = contact.getCustomer();
        customerContactRepository.delete(contact);
        touchCompany(company, null);
    }

    @Override
    public CustomerFollowupResponse addFollowup(Long customerId, CustomerFollowupRequest request, String operatorName) {
        CustomerCompany company = findCompany(customerId);
        CustomerFollowup followup = new CustomerFollowup();
        followup.setCustomer(company);
        applyFollowup(followup, request, operatorName);
        CustomerFollowup saved = customerFollowupRepository.save(followup);
        touchCompany(company, operatorName);
        return toFollowupResponse(saved);
    }

    @Override
    public CustomerFollowupResponse updateFollowup(Long followupId, CustomerFollowupRequest request, String operatorName) {
        CustomerFollowup followup = findFollowup(followupId);
        applyFollowup(followup, request, operatorName);
        CustomerFollowup saved = customerFollowupRepository.save(followup);
        touchCompany(saved.getCustomer(), operatorName);
        return toFollowupResponse(saved);
    }

    @Override
    public void deleteFollowup(Long followupId) {
        CustomerFollowup followup = findFollowup(followupId);
        CustomerCompany company = followup.getCustomer();
        customerFollowupRepository.delete(followup);
        touchCompany(company, null);
    }

    private CustomerCompany findCompany(Long id) {
        return customerCompanyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("客户不存在"));
    }

    private CustomerContact findContact(Long id) {
        return customerContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("联系人不存在"));
    }

    private CustomerFollowup findFollowup(Long id) {
        return customerFollowupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("跟进记录不存在"));
    }

    private void applyCompany(CustomerCompany company, CustomerCompanyRequest request, String operatorName) {
        company.setName(request.getName());
        company.setShortName(request.getShortName());
        company.setIndustry(request.getIndustry());
        company.setCustomerType(request.getCustomerType());
        company.setLevel(request.getLevel());
        company.setRegion(request.getRegion());
        company.setAddress(request.getAddress());
        company.setWebsite(request.getWebsite());
        company.setMainPhone(request.getMainPhone());
        company.setEmail(request.getEmail());
        company.setOwnerName(request.getOwnerName());
        company.setSource(request.getSource());
        company.setStatus(request.getStatus());
        company.setCooperationStage(request.getCooperationStage());
        company.setTags(request.getTags());
        company.setNotes(request.getNotes());
        company.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : company.getCreatedBy());
        company.setUpdatedAt(LocalDateTime.now());
    }

    private void applyContact(CustomerContact contact, CustomerContactRequest request, String operatorName, boolean keepCreator) {
        contact.setName(request.getName());
        contact.setPosition(request.getPosition());
        contact.setDepartment(request.getDepartment());
        contact.setGender(request.getGender());
        contact.setMobile(request.getMobile());
        contact.setOfficePhone(request.getOfficePhone());
        contact.setEmail(request.getEmail());
        contact.setWechat(request.getWechat());
        contact.setQq(request.getQq());
        contact.setDecisionLevel(request.getDecisionLevel());
        contact.setPrimaryContact(Boolean.TRUE.equals(request.getPrimaryContact()));
        contact.setNotes(request.getNotes());
        if (!keepCreator || !StringUtils.hasText(contact.getCreatedBy())) {
            contact.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
        contact.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : contact.getCreatedBy());
        contact.setUpdatedAt(LocalDateTime.now());
    }

    private void applyFollowup(CustomerFollowup followup, CustomerFollowupRequest request, String operatorName) {
        followup.setFollowupType(request.getFollowupType());
        followup.setContent(request.getContent());
        followup.setOwnerName(request.getOwnerName());
        followup.setNextFollowupDate(request.getNextFollowupDate());
        followup.setResultLevel(request.getResultLevel());
        followup.setProjectId(request.getProjectId());
        followup.setProjectName(request.getProjectName());
        followup.setFollowupTime(request.getFollowupTime() != null ? request.getFollowupTime() : LocalDateTime.now());
        if (!StringUtils.hasText(followup.getCreatedBy())) {
            followup.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
    }

    private void touchCompany(CustomerCompany company, String operatorName) {
        company.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : company.getUpdatedBy());
        company.setUpdatedAt(LocalDateTime.now());
        customerCompanyRepository.save(company);
    }

    private CustomerCompanyResponse toResponse(CustomerCompany company) {
        List<CustomerContactResponse> contacts = customerContactRepository.findAllByCustomerIdOrderByPrimaryContactDescUpdatedAtDesc(company.getId())
                .stream()
                .map(this::toContactResponse)
                .toList();
        List<CustomerFollowupResponse> followups = customerFollowupRepository.findAllByCustomerIdOrderByFollowupTimeDesc(company.getId())
                .stream()
                .map(this::toFollowupResponse)
                .toList();
        java.time.LocalDate nextFollowupDate = followups.stream()
                .map(CustomerFollowupResponse::nextFollowupDate)
                .filter(date -> date != null && !date.isBefore(java.time.LocalDate.now()))
                .sorted()
                .findFirst()
                .orElse(null);
        String healthLevel = evaluateHealthLevel(company, followups);
        return new CustomerCompanyResponse(
                company.getId(),
                company.getName(),
                company.getShortName(),
                company.getIndustry(),
                company.getCustomerType(),
                company.getLevel(),
                company.getRegion(),
                company.getAddress(),
                company.getWebsite(),
                company.getMainPhone(),
                company.getEmail(),
                company.getOwnerName(),
                company.getSource(),
                company.getStatus(),
                company.getCooperationStage(),
                company.getTags(),
                company.getNotes(),
                company.getCreatedBy(),
                company.getUpdatedBy(),
                company.getCreatedAt(),
                company.getUpdatedAt(),
                healthLevel,
                nextFollowupDate,
                contacts,
                followups
        );
    }

    private CustomerContactResponse toContactResponse(CustomerContact contact) {
        return new CustomerContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPosition(),
                contact.getDepartment(),
                contact.getGender(),
                contact.getMobile(),
                contact.getOfficePhone(),
                contact.getEmail(),
                contact.getWechat(),
                contact.getQq(),
                contact.getDecisionLevel(),
                contact.getPrimaryContact(),
                contact.getNotes(),
                contact.getCreatedBy(),
                contact.getUpdatedBy(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }

    private CustomerFollowupResponse toFollowupResponse(CustomerFollowup followup) {
        return new CustomerFollowupResponse(
                followup.getId(),
                followup.getFollowupType(),
                followup.getContent(),
                followup.getOwnerName(),
                followup.getNextFollowupDate(),
                followup.getResultLevel(),
                followup.getProjectId(),
                followup.getProjectName(),
                followup.getFollowupTime(),
                followup.getCreatedBy(),
                followup.getCreatedAt()
        );
    }

    private String evaluateHealthLevel(CustomerCompany company, List<CustomerFollowupResponse> followups) {
        if (!followups.isEmpty()) {
            CustomerFollowupResponse latest = followups.get(0);
            if ("高".equals(latest.resultLevel()) || "推进中".equals(company.getStatus())) return "健康";
            if ("中".equals(latest.resultLevel())) return "一般";
        }
        return "关注";
    }

    private boolean matches(String value, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean equalsValue(String value, String expected) {
        return !StringUtils.hasText(expected) || expected.trim().equals(value);
    }
}
