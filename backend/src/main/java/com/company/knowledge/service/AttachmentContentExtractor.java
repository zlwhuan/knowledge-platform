package com.company.knowledge.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for extracting text content from various file formats
 */
@Service
public class AttachmentContentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentContentExtractor.class);

    /**
     * Extract text content from a file
     * @param filePath Path to the file
     * @param contentType MIME type of the file
     * @return Extracted text content, or empty string if extraction fails
     */
    public String extractContent(String filePath, String contentType) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.warn("File does not exist: {}", filePath);
            return "";
        }

        String fileName = path.getFileName().toString().toLowerCase();
        
        try {
            if (fileName.endsWith(".md") || fileName.endsWith(".txt") || fileName.endsWith(".markdown")) {
                return extractTextFile(path);
            } else if (fileName.endsWith(".docx")) {
                return extractDocx(path);
            } else if (fileName.endsWith(".pdf")) {
                return extractPdf(path);
            } else if (fileName.endsWith(".xls")) {
                return extractExcelOld(path);
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm")) {
                return extractExcel(path);
            } else {
                logger.debug("Unsupported file format: {}", fileName);
                return "";
            }
        } catch (Exception e) {
            logger.error("Failed to extract content from file: {}", filePath, e);
            return "";
        }
    }

    /**
     * Extract content from a plain text file
     */
    private String extractTextFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Extract content from a Word document (.docx)
     */
    private String extractDocx(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (InputStream is = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(is)) {
            
            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            
            // Extract tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null) {
                            cells.add(cellText.trim());
                        }
                    }
                    if (!cells.isEmpty()) {
                        content.append(String.join(" | ", cells)).append("\n");
                    }
                }
            }
        }
        
        return content.toString();
    }

    /**
     * Extract content from a PDF file
     * Note: This is a basic implementation. For production, consider using Apache PDFBox.
     */
    private String extractPdf(Path path) throws IOException {
        // For now, return a placeholder. In production, use Apache PDFBox or similar.
        // You can add PDFBox dependency and implement proper PDF extraction.
        logger.info("PDF extraction not fully implemented. File: {}", path.getFileName());
        
        // Try to read as text (some PDFs are text-based)
        try {
            byte[] bytes = Files.readAllBytes(path);
            String text = new String(bytes, StandardCharsets.UTF_8);
            // Remove non-printable characters
            text = text.replaceAll("[^\\x20-\\x7E\\n\\r\\t]", "");
            if (text.length() > 100) {
                return text;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return "[PDF content extraction not available]";
    }

    /**
     * Extract content from an old Excel file (.xls)
     */
    private String extractExcelOld(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (InputStream is = Files.newInputStream(path);
             Workbook workbook = new HSSFWorkbook(is)) {
            content.append(excelWorkbookToString(workbook));
        }
        
        return content.toString();
    }

    /**
     * Extract content from an Excel file (.xlsx, .xlsm)
     */
    private String extractExcel(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (InputStream is = Files.newInputStream(path);
             Workbook workbook = new XSSFWorkbook(is)) {
            content.append(excelWorkbookToString(workbook));
        }
        
        return content.toString();
    }

    /**
     * Common method to extract text from Excel workbook
     */
    private String excelWorkbookToString(Workbook workbook) {
        StringBuilder content = new StringBuilder();
        
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            String sheetName = sheet.getSheetName();
            content.append("# ").append(sheetName).append("\n");
            
            // Find header row
            Row headerRow = null;
            int dataStartRow = 0;
            
            // Look for header in first few rows
            for (int i = 0; i < Math.min(8, sheet.getLastRowNum() + 1); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    int nonEmptyCount = 0;
                    int shortCount = 0;
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                            nonEmptyCount++;
                            if (getCellValue(cell).trim().length() <= 12) {
                                shortCount++;
                            }
                        }
                    }
                    if (nonEmptyCount >= 3 && shortCount >= Math.max(2, nonEmptyCount / 2)) {
                        headerRow = row;
                        dataStartRow = i + 1;
                        break;
                    }
                }
            }
            
            // Extract header
            if (headerRow != null) {
                List<String> headerCells = new ArrayList<>();
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell cell = headerRow.getCell(j);
                    String value = cell != null ? getCellValue(cell).trim() : "";
                    headerCells.add(value);
                }
                content.append(String.join(" | ", headerCells)).append("\n");
            }
            
            // Extract data rows
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                List<String> rowCells = new ArrayList<>();
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String value = cell != null ? getCellValue(cell).trim() : "";
                    rowCells.add(value);
                }
                
                // Skip empty rows
                if (rowCells.stream().allMatch(String::isEmpty)) {
                    continue;
                }
                
                content.append(String.join(" | ", rowCells)).append("\n");
            }
        }
        
        return content.toString();
    }

    /**
     * Get cell value as string
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        return "";
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Extract content from a file with automatic format detection
     * @param file File to extract content from
     * @return Extracted text content
     */
    public String extractContent(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        return extractContent(file.getAbsolutePath(), null);
    }
}