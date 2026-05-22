package com.company.knowledge.controller;

import com.company.knowledge.entity.Project;
import com.company.knowledge.repository.ProjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ProjectRepository projectRepository;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ExportController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping("/projects")
    public ResponseEntity<byte[]> exportProjects() throws IOException {
        List<Project> projects = projectRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("项目列表");
            String[] headers = {"项目名称", "客户名称", "阶段", "状态", "进度%", "合同金额", "已回款", "风险等级",
                "合同状态", "回款状态", "验收状态", "服务状态", "销售负责人", "项目经理", "实施负责人",
                "开始日期", "计划结束日期", "创建时间", "更新时间"};

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Project p : projects) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getName());
                row.createCell(1).setCellValue(p.getCustomerName());
                row.createCell(2).setCellValue(p.getStage());
                row.createCell(3).setCellValue(p.getStatus());
                row.createCell(4).setCellValue(p.getProgress());
                row.createCell(5).setCellValue(p.getContractAmount() != null ? p.getContractAmount().doubleValue() : 0);
                row.createCell(6).setCellValue(p.getReceivedAmount() != null ? p.getReceivedAmount().doubleValue() : 0);
                row.createCell(7).setCellValue(p.getRiskLevel());
                row.createCell(8).setCellValue(p.getContractStatus());
                row.createCell(9).setCellValue(p.getPaymentStatus());
                row.createCell(10).setCellValue(p.getAcceptanceStatus());
                row.createCell(11).setCellValue(p.getServiceStatus());
                row.createCell(12).setCellValue(p.getSalesOwner());
                row.createCell(13).setCellValue(p.getProjectManager());
                row.createCell(14).setCellValue(p.getImplementationOwner());
                row.createCell(15).setCellValue(p.getStartDate() != null ? p.getStartDate().format(DATE_FMT) : "");
                row.createCell(16).setCellValue(p.getPlannedEndDate() != null ? p.getPlannedEndDate().format(DATE_FMT) : "");
                row.createCell(17).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                row.createCell(18).setCellValue(p.getUpdatedAt() != null ? p.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();

            String filename = URLEncoder.encode("项目列表.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }
    }
}
