package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.repository.CustomerCompanyRepository;
import com.company.knowledge.repository.ProjectRepository;
import com.company.knowledge.service.KnowledgeItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final KnowledgeItemService knowledgeItemService;
    private final ProjectRepository projectRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

    public SearchController(KnowledgeItemService knowledgeItemService,
                           ProjectRepository projectRepository,
                           CustomerCompanyRepository customerCompanyRepository) {
        this.knowledgeItemService = knowledgeItemService;
        this.projectRepository = projectRepository;
        this.customerCompanyRepository = customerCompanyRepository;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> globalSearch(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ApiResponse.ok("关键词至少2个字符", Map.of("items", List.of(), "projects", List.of(), "customers", List.of()));
        }
        String keyword = q.trim();

        // Search knowledge items (limit 10)
        List<?> items = knowledgeItemService.list(keyword, null, null, null).stream().limit(10).toList();

        // Search projects by name/customerName
        List<?> projects = projectRepository.findAll().stream()
                .filter(p -> (p.getName() != null && p.getName().contains(keyword))
                        || (p.getCustomerName() != null && p.getCustomerName().contains(keyword)))
                .map(p -> Map.of("id", p.getId(), "name", p.getName(), "customerName", p.getCustomerName(), "stage", p.getStage()))
                .limit(10)
                .toList();

        // Search customers
        List<?> customers = customerCompanyRepository.findAll().stream()
                .filter(c -> (c.getName() != null && c.getName().contains(keyword))
                        || (c.getShortName() != null && c.getShortName().contains(keyword)))
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "industry", c.getIndustry()))
                .limit(10)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("projects", projects);
        result.put("customers", customers);
        return ApiResponse.ok(result);
    }
}
