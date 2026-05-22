package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.SystemOverviewResponse;
import com.company.knowledge.repository.CategoryRepository;
import com.company.knowledge.repository.KnowledgeItemRepository;
import com.company.knowledge.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userAccountRepository;

    @Value("${spring.application.name:knowledge-platform}")
    private String appName;

    @Value("${preview.office.command:}")
    private String officeCommand;

    @Value("${preview.onlyoffice.enabled:false}")
    private boolean onlyOfficeEnabled;

    @Value("${server.port:8080}")
    private String serverPort;

    public SystemController(
            KnowledgeItemRepository knowledgeItemRepository,
            CategoryRepository categoryRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.categoryRepository = categoryRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/overview")
    public ApiResponse<SystemOverviewResponse> overview() {
        SystemOverviewResponse response = new SystemOverviewResponse(
                appName,
                officeCommand,
                onlyOfficeEnabled,
                serverPort,
                knowledgeItemRepository.count(),
                categoryRepository.count(),
                userAccountRepository.count()
        );
        return ApiResponse.ok(response);
    }
}
