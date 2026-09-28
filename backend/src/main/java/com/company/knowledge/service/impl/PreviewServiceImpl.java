package com.company.knowledge.service.impl;

import com.company.knowledge.dto.PreviewMetaResponse;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.AttachmentRepository;
import com.company.knowledge.service.PreviewService;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class PreviewServiceImpl implements PreviewService {

    private static final Duration OFFICE_TIMEOUT = Duration.ofSeconds(90);

    private final AttachmentRepository attachmentRepository;
    private final Path previewRoot = Paths.get("previews");

    @Value("${preview.onlyoffice.enabled:false}")
    private boolean onlyOfficeEnabled;

    @Value("${preview.onlyoffice.server-url:}")
    private String onlyOfficeServerUrl;

    @Value("${preview.onlyoffice.public-base-url:}")
    private String onlyOfficePublicBaseUrl;

    @Value("${preview.office.command:}")
    private String officeCommand;

    public PreviewServiceImpl(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
        try {
            Files.createDirectories(previewRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("无法初始化预览目录", ex);
        }
    }

    @Override
    public PreviewMetaResponse getPreviewMeta(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        PreviewResolution resolution = resolvePreview(attachment);
        String previewUrl = resolution.externalUrl != null
                ? resolution.externalUrl
                : "/api/attachments/" + attachmentId + "/preview/file";
        String onlyOfficeApiUrl = null;
        String onlyOfficeDocumentUrl = null;
        String onlyOfficeFileType = null;
        String onlyOfficeDocumentKey = null;
        if ("onlyoffice".equals(resolution.kind)) {
            String base = normalizeBaseUrl(onlyOfficeServerUrl);
            String publicBase = normalizeBaseUrl(onlyOfficePublicBaseUrl);
            onlyOfficeApiUrl = base == null ? null : base + "/web-apps/apps/api/documents/api.js";
            onlyOfficeDocumentUrl = publicBase == null ? null : publicBase + "/api/attachments/" + attachmentId + "/download";
            onlyOfficeFileType = detectOfficeFileType(attachment.getOriginalFileName());
            onlyOfficeDocumentKey = attachmentId + "-" + attachment.getUploadedAt();
        }
        return new PreviewMetaResponse(
                resolution.kind,
                previewUrl,
                attachment.getOriginalFileName(),
                resolution.contentType,
                attachment.getFileSize(),
                resolution.message,
                onlyOfficeApiUrl,
                onlyOfficeDocumentUrl,
                onlyOfficeFileType,
                onlyOfficeDocumentKey
        );
    }

    @Override
    public Resource getPreviewResource(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        PreviewResolution resolution = resolvePreview(attachment);
        if (resolution.path == null) {
            throw new ResourceNotFoundException("当前预览使用外部渲染地址");
        }
        Path target = resolution.path;
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("预览文件不存在");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("预览文件路径无效", ex);
        }
    }

    @Override
    public String getPreviewContentType(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        return resolvePreview(attachment).contentType;
    }

    @Override
    public PreviewMetaResponse getSessionAttachmentPreviewMeta(String fileName, String contentType, String filePath, String previewKey) {
        Path source = Paths.get(filePath);
        PreviewResolution resolution = resolvePreview(fileName, contentType, source, previewKey, false, null);
        String previewUrl = resolution.externalUrl != null
                ? resolution.externalUrl
                : "/api/skills/session-attachments/" + previewKey + "/preview/file";
        return new PreviewMetaResponse(
                resolution.kind,
                previewUrl,
                fileName,
                resolution.contentType,
                Files.exists(source) ? source.toFile().length() : 0L,
                resolution.message,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public Resource getSessionAttachmentPreviewResource(String fileName, String contentType, String filePath, String previewKey) {
        Path source = Paths.get(filePath);
        PreviewResolution resolution = resolvePreview(fileName, contentType, source, previewKey, false, null);
        if (resolution.path == null) {
            throw new ResourceNotFoundException("当前预览使用外部渲染地址");
        }
        try {
            Resource resource = new UrlResource(resolution.path.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("预览文件不存在");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("预览文件路径无效", ex);
        }
    }

    @Override
    public String getSessionAttachmentContentType(String fileName, String contentType, String filePath, String previewKey) {
        Path source = Paths.get(filePath);
        return resolvePreview(fileName, contentType, source, previewKey, false, null).contentType;
    }

    private PreviewResolution resolvePreview(Attachment attachment) {
        return resolvePreview(
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                Paths.get(attachment.getFilePath()),
                String.valueOf(attachment.getId()),
                true,
                attachment);
    }

    /** 会话临时附件：复用同一套预览策略，cacheKey 用于预览产物目录 */
    private PreviewResolution resolvePreview(String fileName, String contentType, Path source, String cacheKey,
                                             boolean allowOnlyOffice, Attachment attachmentOrNull) {
        String kind = detectKind(fileName, contentType);

        // Excel：HTML 表格（保列/样式/图）；Word/PPT：LibreOffice PDF（版式最好）
        if ("office-sheet".equals(kind)) {
            try {
                Path html = buildHtmlPreview(source, kind, cacheKey);
                return new PreviewResolution("html", html, "text/html; charset=UTF-8",
                        "已生成离线表格预览（保留全部列与样式，可横向滚动）");
            } catch (Exception ex) {
                // 极端损坏文件再退回 PDF
            }
        }

        if ("office-word".equals(kind) || "office-slide".equals(kind) || "office-sheet".equals(kind)) {
            Path pdf = convertOfficeToPdf(source, cacheKey);
            if (pdf != null) {
                return new PreviewResolution(
                        "pdf",
                        pdf,
                        "application/pdf",
                        "已通过 LibreOffice 离线转换为 PDF 预览，尽可能还原原始版式"
                );
            }

            if (allowOnlyOffice && attachmentOrNull != null) {
                String onlyOfficeUrl = buildOnlyOfficeUrl(attachmentOrNull);
                if (onlyOfficeUrl != null) {
                    return new PreviewResolution(
                            "onlyoffice",
                            null,
                            "text/html; charset=UTF-8",
                            "LibreOffice 转换失败，已回退为 OnlyOffice 文档预览"
                    ).withExternalUrl(onlyOfficeUrl);
                }
            }

            Path html = buildHtmlPreview(source, kind, cacheKey);
            return new PreviewResolution(
                    kind,
                    html,
                    "text/html; charset=UTF-8",
                    "LibreOffice/OnlyOffice 均不可用，已降级为结构化文本预览"
            );
        }

        if ("markdown".equals(kind) || "html".equals(kind) || "text".equals(kind)) {
            return new PreviewResolution(kind, buildHtmlPreview(source, kind, cacheKey), "text/html; charset=UTF-8", "已生成离线预览页面");
        }

        if ("image".equals(kind) || "video".equals(kind) || "audio".equals(kind) || "pdf".equals(kind)) {
            String type = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
            return new PreviewResolution(kind, source, type, "原始文件可直接离线预览");
        }

        return new PreviewResolution("fallback", source, "application/octet-stream", "该文件暂不支持结构化预览，可直接下载查看");
    }

    private Path buildHtmlPreview(Path source, String kind, String cacheKey) {
        try {
            Path dir = previewRoot.resolve("attachment-" + cacheKey);
            Files.createDirectories(dir);
            Path target = dir.resolve("preview.html");
            if (Files.exists(target) && Files.getLastModifiedTime(target).toMillis() >= Files.getLastModifiedTime(source).toMillis()) {
                return target;
            }

            switch (kind) {
                case "office-word" -> Files.writeString(target, wrapHtml(extractWord(source), false), StandardCharsets.UTF_8);
                case "office-sheet" -> Files.writeString(target, wrapHtml(extractSheet(source, dir), true), StandardCharsets.UTF_8);
                case "office-slide" -> Files.writeString(target, wrapHtml(extractSlide(source), false), StandardCharsets.UTF_8);
                case "html" -> {
                    String htmlContent = Files.readString(source, StandardCharsets.UTF_8);
                    Files.writeString(target, fixRelativePaths(htmlContent, cacheKey), StandardCharsets.UTF_8);
                }
                case "markdown" -> Files.writeString(target, wrapHtml(renderPlainText(Files.readString(source)), false), StandardCharsets.UTF_8);
                case "text" -> Files.writeString(target, wrapHtml(renderTextByExtension(source), source.toString().toLowerCase().endsWith(".csv")), StandardCharsets.UTF_8);
                default -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("生成预览文件失败", ex);
        }
    }

    private Path convertOfficeToPdf(Path source, String cacheKey) {
        try {
            Path dir = previewRoot.resolve("attachment-" + cacheKey);
            Files.createDirectories(dir);
            Path target = dir.resolve("preview.pdf");
            if (Files.exists(target) && Files.getLastModifiedTime(target).toMillis() >= Files.getLastModifiedTime(source).toMillis()) {
                return target;
            }

            String baseName = source.getFileName().toString();
            int index = baseName.lastIndexOf('.');
            if (index > 0) {
                baseName = baseName.substring(0, index);
            }
            Path converted = dir.resolve(baseName + ".pdf");

            String sourceLower = source.getFileName().toString().toLowerCase();
            String pdfFilter = sourceLower.matches(".*\\.(xls|xlsx)$") ? "calc_pdf_Export" : "writer_pdf_Export";

            for (String command : resolveOfficeCommands()) {
                Path officeProfileDir = previewRoot.resolve("office-profile-shared");
                Files.createDirectories(officeProfileDir);
                ProcessBuilder processBuilder = new ProcessBuilder(
                        command,
                        "--headless",
                        "--invisible",
                        "--nologo",
                        "--nodefault",
                        "--nolockcheck",
                        "--norestore",
                        "--nofirststartwizard",
                        "-env:UserInstallation=file:///" + officeProfileDir.toAbsolutePath().toString().replace('\\', '/'),
                        "--convert-to",
                        "pdf:" + pdfFilter,
                        "--outdir",
                        dir.toAbsolutePath().toString(),
                        source.toAbsolutePath().toString()
                );
                processBuilder.environment().put("SAL_DISABLE_SYNCHRONOUS_PRINTER_DETECTION", "1");
                processBuilder.redirectErrorStream(true);
                try {
                    Process process = processBuilder.start();
                    boolean finished = process.waitFor(OFFICE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        continue;
                    }
                    if (process.exitValue() != 0) {
                        continue;
                    }
                    if (!Files.exists(converted)) {
                        continue;
                    }
                    Files.move(converted, target, StandardCopyOption.REPLACE_EXISTING);
                    return target;
                } catch (IOException ex) {
                    continue;
                }
            }
            return null;
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Word 兜底 HTML：按文档流输出段落/标题/表格（LibreOffice 不可用时） */
    private String extractWord(Path source) throws IOException {
        String lower = source.getFileName().toString().toLowerCase();
        if (lower.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(source); XWPFDocument document = new XWPFDocument(in)) {
                StringBuilder body = new StringBuilder();
                for (IBodyElement el : document.getBodyElements()) {
                    if (el instanceof XWPFParagraph p) {
                        String text = p.getText();
                        if (text == null || text.isBlank()) continue;
                        String style = p.getStyle() == null ? "" : p.getStyle().toLowerCase();
                        String html = inlineRuns(p);
                        if (style.contains("heading") || style.matches("^[1-9]$")) {
                            int level = 1;
                            java.util.regex.Matcher hm = java.util.regex.Pattern.compile("(\\d)").matcher(style);
                            if (hm.find()) level = Math.min(6, Math.max(1, Integer.parseInt(hm.group(1))));
                            body.append("<h").append(level).append(">").append(html).append("</h").append(level).append(">");
                        } else if (p.getNumFmt() != null || p.getNumID() != null) {
                            body.append("<li>").append(html).append("</li>");
                        } else {
                            body.append("<p>").append(html).append("</p>");
                        }
                    } else if (el instanceof XWPFTable table) {
                        body.append("<div class=\"sheet-wrap\"><table>");
                        for (XWPFTableRow row : table.getRows()) {
                            body.append("<tr>");
                            for (var cell : row.getTableCells()) {
                                StringBuilder cellHtml = new StringBuilder();
                                for (XWPFParagraph cp : cell.getParagraphs()) {
                                    if (cp.getText() != null && !cp.getText().isBlank()) {
                                        cellHtml.append(inlineRuns(cp)).append("<br/>");
                                    }
                                }
                                if (cellHtml.length() >= 5) cellHtml.setLength(cellHtml.length() - 5);
                                body.append("<td>").append(cellHtml).append("</td>");
                            }
                            body.append("</tr>");
                        }
                        body.append("</table></div>");
                    }
                }
                return body.toString();
            }
        }
        try (InputStream in = Files.newInputStream(source); HWPFDocument document = new HWPFDocument(in); WordExtractor extractor = new WordExtractor(document)) {
            return renderPlainText(extractor.getText());
        }
    }

    /** 段落 runs → HTML（加粗/斜体/下划线/颜色） */
    private String inlineRuns(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        if (p.getRuns().isEmpty()) {
            return escape(p.getText() == null ? "" : p.getText());
        }
        for (XWPFRun run : p.getRuns()) {
            String t = run.text();
            if (t == null || t.isEmpty()) continue;
            String piece = escape(t);
            if (run.isBold()) piece = "<strong>" + piece + "</strong>";
            if (run.isItalic()) piece = "<em>" + piece + "</em>";
            if (run.getUnderline() != null && run.getUnderline() != UnderlinePatterns.NONE) {
                piece = "<u>" + piece + "</u>";
            }
            sb.append(piece);
        }
        return sb.toString();
    }

    private String extractSheet(Path source, Path assetDir) throws IOException {
        // WPS _xlfn.DISPIMG 单元格图：先从 xlsx 包里挖 ID→图
        Map<String, String> wpsImages = extractWpsDispImages(source);
        try (InputStream in = Files.newInputStream(source); Workbook workbook = WorkbookFactory.create(in)) {
            DataFormatter formatter = new DataFormatter();
            int sheetCount = workbook.getNumberOfSheets();
            StringBuilder tabs = new StringBuilder();
            StringBuilder panes = new StringBuilder();
            for (int i = 0; i < sheetCount; i++) {
                var sheet = workbook.getSheetAt(i);
                int maxCols = 0;
                for (Row row : sheet) {
                    if (row != null && row.getLastCellNum() > maxCols) {
                        maxCols = row.getLastCellNum();
                    }
                }
                // 合并单元格：起点带 rowspan/colspan，从属格跳过
                Map<String, org.apache.poi.ss.util.CellRangeAddress> mergeStart = new LinkedHashMap<>();
                java.util.Set<String> mergeCovered = new java.util.HashSet<>();
                try {
                    for (org.apache.poi.ss.util.CellRangeAddress region : sheet.getMergedRegions()) {
                        mergeStart.put(region.getFirstRow() + ":" + region.getFirstColumn(), region);
                        if (region.getLastColumn() + 1 > maxCols) {
                            maxCols = region.getLastColumn() + 1;
                        }
                        for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                            for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                                if (r != region.getFirstRow() || c != region.getFirstColumn()) {
                                    mergeCovered.add(r + ":" + c);
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // ignore
                }

                Map<String, String> cellImages = extractSheetImages(workbook, sheet, i, assetDir, maxCols);
                // DISPIMG 单元格：公式文本换成图
                java.util.List<String> dispCells = new ArrayList<>();
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    for (int c = 0; c < maxCols; c++) {
                        var cell = row.getCell(c);
                        if (cell == null) continue;
                        String v = formatter.formatCellValue(cell);
                        if (v != null && v.contains("DISPIMG")) {
                            dispCells.add(r + ":" + c);
                        }
                    }
                }
                if (!wpsImages.isEmpty() && !dispCells.isEmpty()) {
                    int n = 0;
                    for (String id : wpsImages.keySet()) {
                        if (n >= dispCells.size()) break;
                        String key = dispCells.get(n++);
                        if (!cellImages.containsKey(key)) {
                            cellImages.put(key, wpsImages.get(id));
                        }
                    }
                    // 还有图没对上格子：挂到末行，保证不丢
                    if (n < wpsImages.size()) {
                        var ids = new ArrayList<>(wpsImages.keySet());
                        for (int k = n; k < ids.size(); k++) {
                            cellImages.put(sheet.getLastRowNum() + ":" + Math.max(maxCols - 1, 0) + "-extra" + k,
                                    wpsImages.get(ids.get(k)));
                        }
                    }
                }

                String sheetName = sheet.getSheetName() == null ? ("Sheet" + (i + 1)) : sheet.getSheetName();
                tabs.append("<button type=\"button\" class=\"sheet-tab")
                        .append(i == 0 ? " active" : "")
                        .append("\" data-sheet=\"").append(i).append("\">")
                        .append(escape(sheetName))
                        .append("</button>");
                panes.append("<section class=\"sheet-pane").append(i == 0 ? " active" : "")
                        .append("\" data-sheet=\"").append(i).append("\">")
                        .append("<div class=\"sheet-meta\">").append(maxCols).append(" 列 · ")
                        .append(sheet.getLastRowNum() + 1).append(" 行")
                        .append(cellImages.isEmpty() ? "" : " · " + cellImages.size() + " 图")
                        .append("</div>")
                        .append("<div class=\"sheet-wrap\"><table><tbody>");
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    panes.append("<tr>");
                    for (int c = 0; c < maxCols; c++) {
                        if (mergeCovered.contains(r + ":" + c)) {
                            continue; // 合并从属格不输出
                        }
                        org.apache.poi.ss.usermodel.Cell cell = row == null ? null : row.getCell(c);
                        String val = "";
                        if (cell != null) {
                            val = formatter.formatCellValue(cell);
                        }
                        // WPS 图公式文本一律不露出（含 _xlfn.DISPIMG("ID_…",1) 变体）
                        if (val != null) {
                            val = val.replaceAll("(?i)_xlfn\\.DISPIMG\\([^)]*\\)", "")
                                     .replaceAll("(?i)DISPIMG\\([^)]*\\)", "")
                                     .trim();
                        }
                        String img = cellImages.get(r + ":" + c);
                        // 同格多图（extra 后缀）
                        StringBuilder imgs = new StringBuilder();
                        if (img != null) {
                            imgs.append("<div class=\"sheet-img\"><img src=\"").append(img).append("\" alt=\"内嵌图片\" /></div>");
                        }
                        for (Map.Entry<String, String> e : cellImages.entrySet()) {
                            if (e.getKey().startsWith(r + ":" + c + "-")) {
                                imgs.append("<div class=\"sheet-img\"><img src=\"").append(e.getValue()).append("\" alt=\"内嵌图片\" /></div>");
                            }
                        }
                        String css = cellStyleToCss(workbook, cell);
                        String span = "";
                        var region = mergeStart.get(r + ":" + c);
                        if (region != null) {
                            int rs = region.getLastRow() - region.getFirstRow() + 1;
                            int cs = region.getLastColumn() - region.getFirstColumn() + 1;
                            if (rs > 1) span += " rowspan=\"" + rs + "\"";
                            if (cs > 1) span += " colspan=\"" + cs + "\"";
                        }
                        panes.append("<td").append(span).append(css.isEmpty() ? "" : " style=\"" + css + "\"").append(">");
                        if (val != null && !val.isEmpty()) {
                            panes.append(escape(val));
                        }
                        panes.append(imgs);
                        panes.append("</td>");
                    }
                    panes.append("</tr>");
                }
                panes.append("</tbody></table></div></section>");
            }
            String js = sheetCount <= 1 ? "" : (
                    "<script>document.querySelectorAll('.sheet-tab').forEach(function(t){t.addEventListener('click',function(){"
                    + "document.querySelectorAll('.sheet-tab').forEach(function(x){x.classList.remove('active')});"
                    + "document.querySelectorAll('.sheet-pane').forEach(function(x){x.classList.remove('active')});"
                    + "t.classList.add('active');"
                    + "var p=document.querySelector('.sheet-pane[data-sheet=\"'+t.dataset.sheet+'\"]');"
                    + "if(p)p.classList.add('active');});});</script>"
            );
            return "<div class=\"sheet-tabs\">" + tabs + "</div>" + panes + js;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * WPS _xlfn.DISPIMG 单元格图：xlsx 即 zip，扫 xl/media 并按 ID/顺序映射，返回 id → dataURI。
     */
    private Map<String, String> extractWpsDispImages(Path source) {
        Map<String, String> byId = new LinkedHashMap<>();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(source.toFile())) {
            java.util.List<java.util.zip.ZipEntry> media = new ArrayList<>();
            java.util.List<java.util.zip.ZipEntry> all = new ArrayList<>();
            java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                all.add(en.nextElement());
            }
            for (java.util.zip.ZipEntry e : all) {
                String n = e.getName().toLowerCase();
                if (n.startsWith("xl/media/") && !e.isDirectory()
                        && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".emf") || n.endsWith(".wmf"))) {
                    media.add(e);
                }
            }
            // ID 映射：扫 xml 里 ID_xxx → media 路径
            Map<String, String> idToMediaPath = new LinkedHashMap<>();
            java.util.regex.Pattern idPat = java.util.regex.Pattern.compile("ID_[A-Za-z0-9_]+");
            for (java.util.zip.ZipEntry e : all) {
                String n = e.getName();
                if (!n.endsWith(".xml") && !n.endsWith(".vml") && !n.endsWith(".rels")) continue;
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(zip.getInputStream(e), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String xml = sb.toString();
                    if (!xml.contains("DISPIMG") && !xml.contains("ID_")) continue;
                    // 同一小段里同时出现 ID_ 与 image 路径
                    java.util.regex.Matcher m = idPat.matcher(xml);
                    while (m.find()) {
                        String id = m.group();
                        int start = Math.max(0, m.start() - 400);
                        int end = Math.min(xml.length(), m.end() + 400);
                        String near = xml.substring(start, end);
                        java.util.regex.Matcher imgM = java.util.regex.Pattern
                                .compile("(?:media/image|/media/)\\d+\\.(?:png|jpe?g|gif|bmp|emf|wmf)", java.util.regex.Pattern.CASE_INSENSITIVE)
                                .matcher(near);
                        if (imgM.find()) {
                            idToMediaPath.put(id, "xl/" + imgM.group());
                        }
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            }
            Map<String, byte[]> mediaData = new LinkedHashMap<>();
            for (var e : media) {
                try (var is = zip.getInputStream(e)) {
                    mediaData.put(e.getName(), is.readAllBytes());
                } catch (Exception ignore) {
                    // ignore
                }
            }
            for (Map.Entry<String, String> mapEn : idToMediaPath.entrySet()) {
                byte[] data = mediaData.get(mapEn.getValue());
                if (data != null) {
                    byId.put(mapEn.getKey(), toDataUri(mapEn.getValue(), data));
                }
            }
            // 兜底：按顺序给剩余 media
            int i = 0;
            for (var e : media) {
                String id = "IMG_" + (i++);
                if (!byId.containsValue(toDataUri(e.getName(), mediaData.getOrDefault(e.getName(), new byte[0])))
                        && mediaData.containsKey(e.getName())) {
                    // 只补还没被 ID 映射用过的图
                    boolean used = byId.values().stream().anyMatch(v -> v.contains(java.util.Base64.getEncoder().encodeToString(
                            mediaData.getOrDefault(e.getName(), new byte[0]))));
                    if (!used) {
                        byId.put(id, toDataUri(e.getName(), mediaData.get(e.getName())));
                    }
                }
            }
            return byId;
        } catch (Exception e) {
            return byId;
        }
    }

    private String toDataUri(String path, byte[] data) {
        if (data == null || data.length == 0) return "";
        String lower = path == null ? "" : path.toLowerCase();
        String mime = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg"
                : lower.endsWith(".gif") ? "image/gif"
                : lower.endsWith(".bmp") ? "image/bmp"
                : "image/png";
        return "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(data);
    }

    /** 单元格样式 → 内联 CSS：字体/加粗/斜体/下划线/颜色/底色/对齐/换行/边框 */
    private String cellStyleToCss(Workbook workbook, org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        try {
            org.apache.poi.ss.usermodel.CellStyle style = cell.getCellStyle();
            if (style == null) return "";
            StringBuilder css = new StringBuilder();

            // 字体
            try {
                org.apache.poi.ss.usermodel.Font font = workbook.getFontAt(style.getFontIndex());
                if (font != null) {
                    if (font.getBold()) css.append("font-weight:700;");
                    if (font.getItalic()) css.append("font-style:italic;");
                    if (font.getUnderline() != org.apache.poi.ss.usermodel.Font.U_NONE) {
                        css.append("text-decoration:underline;");
                    }
                    short h = font.getFontHeightInPoints();
                    if (h > 0) css.append("font-size:").append(Math.min(h, 28)).append("px;");
                    String fc = xssfFontColor(cell);
                    if (fc == null) fc = poiColorToCss(workbook, font.getColor());
                    if (fc != null) css.append("color:").append(fc).append(";");
                }
            } catch (Exception ignore) {
                // ignore
            }

            // 底色：xlsx 用 XSSF RGB，indexed 兜底
            try {
                if (style.getFillPattern() == org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND) {
                    String bg = xssfFillColor(cell);
                    if (bg == null) bg = poiColorToCss(workbook, style.getFillForegroundColor());
                    if (bg != null) css.append("background:").append(bg).append(";");
                }
            } catch (Exception ignore) {
                // ignore
            }

            // 对齐
            switch (style.getAlignment()) {
                case CENTER, CENTER_SELECTION -> css.append("text-align:center;");
                case RIGHT -> css.append("text-align:right;");
                case JUSTIFY -> css.append("text-align:justify;");
                default -> { }
            }
            switch (style.getVerticalAlignment()) {
                case CENTER -> css.append("vertical-align:middle;");
                case BOTTOM -> css.append("vertical-align:bottom;");
                default -> { }
            }
            if (style.getWrapText()) {
                css.append("white-space:normal;overflow-wrap:anywhere;");
            }

            // 边框
            css.append(borderCss("border-top", style.getBorderTop(), style.getTopBorderColor()));
            css.append(borderCss("border-bottom", style.getBorderBottom(), style.getBottomBorderColor()));
            css.append(borderCss("border-left", style.getBorderLeft(), style.getLeftBorderColor()));
            css.append(borderCss("border-right", style.getBorderRight(), style.getRightBorderColor()));

            return css.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String borderCss(String prop, org.apache.poi.ss.usermodel.BorderStyle border, short colorIdx) {
        if (border == null || border == org.apache.poi.ss.usermodel.BorderStyle.NONE) return "";
        String w = (border.getCode() >= 2) ? "2px" : "1px";
        String color = poiColorToCss(null, colorIdx);
        return prop + ":" + w + " solid " + (color == null ? "#cbd5e1" : color) + ";";
    }

    private String poiColorToCss(Workbook workbook, short indexed) {
        // 常用 indexed 调色板兜底（Excel 8/9/10…）；HSSF 自定义色少见，够用
        return switch (indexed) {
            case 8 -> "#000000";
            case 9 -> "#ffffff";
            case 10, 11 -> "#ff0000";
            case 12 -> "#00ff00";
            case 13 -> "#0000ff";
            case 14 -> "#ffff00";
            case 15 -> "#ff00ff";
            case 16 -> "#00ffff";
            case 22 -> "#000080";
            case 23 -> "#008000";
            case 24 -> "#800000";
            case 41 -> "#808080";
            case 42 -> "#c0c0c0";
            case 43 -> "#999999";
            case 64 -> "#000000";
            default -> null;
        };
    }

    private String xssfColorToCss(org.apache.poi.xssf.usermodel.XSSFColor color) {
        if (color == null) return null;
        try {
            byte[] rgb = color.getRGB();
            if (rgb != null && rgb.length >= 3) {
                return String.format("#%02x%02x%02x", rgb[0] & 0xff, rgb[1] & 0xff, rgb[2] & 0xff);
            }
            String argb = color.getARGBHex();
            if (argb != null && argb.length() >= 6) {
                return "#" + argb.substring(argb.length() - 6);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    /** xlsx 字体自定义 RGB */
    private String xssfFontColor(org.apache.poi.ss.usermodel.Cell cell) {
        try {
            if (!(cell instanceof org.apache.poi.xssf.usermodel.XSSFCell xc)) return null;
            org.apache.poi.xssf.usermodel.XSSFCellStyle xs =
                    (org.apache.poi.xssf.usermodel.XSSFCellStyle) xc.getCellStyle();
            return xssfColorToCss(xs.getFont().getXSSFColor());
        } catch (Exception e) {
            return null;
        }
    }

    /** xlsx 单元格填充色（背景色） */
    private String xssfFillColor(org.apache.poi.ss.usermodel.Cell cell) {
        try {
            if (!(cell instanceof org.apache.poi.xssf.usermodel.XSSFCell xc)) return null;
            org.apache.poi.xssf.usermodel.XSSFCellStyle xs =
                    (org.apache.poi.xssf.usermodel.XSSFCellStyle) xc.getCellStyle();
            String fg = xssfColorToCss(xs.getFillForegroundXSSFColor());
            if (fg != null) return fg;
            return xssfColorToCss(xs.getFillBackgroundXSSFColor());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 抽出 Sheet 内嵌图片 → base64 data URI（预览 HTML 单文件可显示，不受相对路径影响）。
     * 无锚点的图挂在最后一列旁，避免丢失。
     */
    private Map<String, String> extractSheetImages(Workbook workbook, org.apache.poi.ss.usermodel.Sheet sheet,
                                                   int sheetIndex, Path assetDir, int maxCols) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!(workbook instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook xssf)) {
            return out;
        }
        var sheetImpl = xssf.getSheetAt(sheetIndex);
        var drawing = sheetImpl.getDrawingPatriarch();
        if (drawing == null) {
            return out;
        }
        int imgSeq = 0;
        for (var shape : drawing.getShapes()) {
            if (!(shape instanceof org.apache.poi.xssf.usermodel.XSSFPicture pic)) {
                continue;
            }
            byte[] data;
            String ext;
            try {
                data = pic.getPictureData().getData();
                ext = pic.getPictureData().suggestFileExtension();
            } catch (Exception e) {
                continue;
            }
            if (data == null || data.length == 0) {
                continue;
            }
            if (ext == null || ext.isBlank()) ext = "png";
            String mime = switch (ext.toLowerCase()) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "gif" -> "image/gif";
                case "bmp" -> "image/bmp";
                case "emf", "wmf" -> "image/png"; // 浏览器不认，仍占位
                default -> "image/png";
            };
            String src = "data:" + mime + ";base64," +
                    java.util.Base64.getEncoder().encodeToString(data);
            String key;
            try {
                org.apache.poi.ss.usermodel.ClientAnchor anchor = pic.getPreferredSize();
                if (anchor == null && pic.getAnchor() instanceof org.apache.poi.ss.usermodel.ClientAnchor ca) {
                    anchor = ca;
                }
                if (anchor != null) {
                    key = anchor.getRow1() + ":" + Math.min(anchor.getCol1(), Math.max(maxCols - 1, 0));
                } else {
                    key = "-1:-1";
                }
            } catch (Exception e) {
                key = "-1:-1";
            }
            if (out.containsKey(key)) {
                key = key + "-" + (imgSeq++);
            } else {
                imgSeq++;
            }
            out.put(key, src);
        }
        if (out.containsKey("-1:-1")) {
            String f = out.remove("-1:-1");
            int lastRow = Math.max(sheet.getLastRowNum(), 0);
            out.put(lastRow + ":" + Math.max(maxCols - 1, 0), f);
        }
        return out;
    }

    private String extractSlide(Path source) throws IOException {
        String lower = source.getFileName().toString().toLowerCase();
        if (lower.endsWith(".pptx")) {
            try (InputStream in = Files.newInputStream(source); XMLSlideShow show = new XMLSlideShow(in)) {
                StringBuilder builder = new StringBuilder();
                show.getSlides().forEach(slide -> {
                    builder.append("<section class=\"slide-card\"><h2>")
                            .append(escape(slide.getTitle() == null ? "幻灯片" : slide.getTitle()))
                            .append("</h2><ul>");
                    slide.getShapes().forEach(shape -> builder.append("<li>").append(escape(shape.getShapeName())).append("</li>"));
                    builder.append("</ul></section>");
                });
                return builder.toString();
            }
        }
        try (InputStream in = Files.newInputStream(source); HSLFSlideShow show = new HSLFSlideShow(in)) {
            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (HSLFSlide slide : show.getSlides()) {
                builder.append("<section class=\"slide-card\"><h2>第 ").append(index++).append(" 页</h2><ul>");
                for (HSLFShape shape : slide.getShapes()) {
                    String text = shape.getShapeName();
                    if (text != null && !text.isBlank()) {
                        builder.append("<li>").append(escape(text)).append("</li>");
                    }
                }
                builder.append("</ul></section>");
            }
            return builder.toString();
        }
    }

    private String renderTextByExtension(Path source) throws IOException {
        String lower = source.getFileName().toString().toLowerCase();
        String content = Files.readString(source, StandardCharsets.UTF_8);
        if (lower.endsWith(".csv")) {
            return renderCsv(content);
        }
        if (lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return "<pre>" + escape(content) + "</pre>";
        }
        return renderPlainText(content);
    }

    private String renderPlainText(String content) {
        return "<pre>" + escape(content) + "</pre>";
    }

    private String renderCsv(String content) {
        StringBuilder builder = new StringBuilder("<div class=\"sheet-wrap\"><table><tbody>");
        String[] rows = content.split("\\r?\\n");
        for (String row : rows) {
            if (row.isBlank()) {
                continue;
            }
            builder.append("<tr>");
            for (String cell : row.split(",", -1)) {
                builder.append("<td>").append(escape(cell.trim())).append("</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</tbody></table></div>");
        return builder.toString();
    }

    private Attachment findAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("附件不存在"));
    }

    private String detectKind(String fileName, String contentType) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        String type = contentType == null ? "" : contentType.toLowerCase();
        if (type.startsWith("image/") || lower.matches(".*\\.(png|jpg|jpeg|gif|bmp|webp|svg)$")) return "image";
        if (type.startsWith("video/") || lower.matches(".*\\.(mp4|webm|ogg|mov|m4v|avi|mkv)$")) return "video";
        if (type.startsWith("audio/") || lower.matches(".*\\.(mp3|wav|ogg|m4a|aac|flac)$")) return "audio";
        if (type.contains("pdf") || lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".html") || lower.endsWith(".htm") || type.contains("html")) return "html";
        if (type.contains("markdown") || lower.endsWith(".md") || lower.endsWith(".markdown")) return "markdown";
        if (lower.matches(".*\\.(doc|docx)$")) return "office-word";
        if (lower.matches(".*\\.(xls|xlsx)$")) return "office-sheet";
        if (lower.matches(".*\\.(ppt|pptx)$")) return "office-slide";
        if (type.startsWith("text/") || lower.matches(".*\\.(txt|csv|json|xml|log|yaml|yml|sh|bash|zsh|fish|csh|ksh|cmd|bat|ps1|py|rb|pl|pm|js|jsx|ts|tsx|java|c|cpp|cc|h|hpp|cs|go|rs|swift|kt|scala|r|m|mm|lua|sql|css|scss|less|sass|styl|ini|cfg|conf|config|env|properties|toml|lock|gitignore|dockerignore|editorconfig|eslintrc|prettierrc|babelrc|npmrc|yarnrc|gradle|groovy|gvy|gy|clj|cljs|cljc|edn|ex|exs|erl|hrl|hs|lhs|ml|mli|fs|fsx|fsi|v|vh|sv|vhdl|vhd|tcl|awk|sed|diff|patch|re|rei|dart|vbs|vb|bas|frm|cls|ctl|pag|dsr|dob|ctl|xaml|wxs|wxl|wxi|pyx|pxd|pxi|coffee|litcoffee|iced|elm|purs|nix|dhall|jsonnet|libsonnet|cue|smithy|graphql|gql|thrift|proto|capnp|avsc|json5|hjson|hcl|tf|tfvars|nomad|pkr|hcl|rego|sv|svh|v|vh|qsf|sdc|srf|ucf)$")) return "text";
        return "fallback";
    }

    private String buildOnlyOfficeUrl(Attachment attachment) {
        if (!onlyOfficeEnabled) {
            return null;
        }
        String server = normalizeBaseUrl(onlyOfficeServerUrl);
        String base = normalizeBaseUrl(onlyOfficePublicBaseUrl);
        if (server == null || base == null) {
            return null;
        }
        String downloadUrl = base + "/api/attachments/" + attachment.getId() + "/download";
        String fileType = detectOfficeFileType(attachment.getOriginalFileName());
        String name = attachment.getOriginalFileName() == null ? "document" : attachment.getOriginalFileName();
        String encodedDownload = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8);
        String encodedTitle = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return server + "/web-apps/apps/documenteditor/main/index.html?fileType=" + fileType + "&title=" + encodedTitle + "&url=" + encodedDownload;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("/+$", "");
    }

    private String detectOfficeFileType(String fileName) {
        String name = fileName == null ? "document.docx" : fileName;
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase();
        }
        return "docx";
    }

    private List<String> resolveOfficeCommands() {
        List<String> commands = new ArrayList<>();
        if (officeCommand != null && !officeCommand.isBlank()) {
            commands.add(officeCommand.trim());
        }
        commands.add("soffice");
        commands.add("soffice.exe");
        commands.add("C:/Program Files/LibreOffice/program/soffice.exe");
        commands.add("C:/Program Files (x86)/LibreOffice/program/soffice.exe");
        return commands.stream().distinct().toList();
    }

    private String wrapHtml(String body, boolean tableMode) {
        String pageClass = tableMode ? "preview-page table-mode" : "preview-page";
        String css = """
                body{margin:0;background:#eef2f6;font-family:Segoe UI,PingFang SC,Microsoft YaHei,sans-serif;color:#1f2937}
                .preview-page{padding:16px 20px;line-height:1.6}
                .preview-page h2{margin:0 0 12px;font-size:15px}
                .sheet-tabs{display:flex;flex-wrap:wrap;gap:6px;margin:0 0 12px}
                .sheet-tab{border:1px solid #c9d4e4;background:#fff;color:#334155;border-radius:8px;padding:6px 12px;font-size:12px;cursor:pointer}
                .sheet-tab.active{background:#2563eb;border-color:#2563eb;color:#fff}
                .sheet-pane{display:none}
                .sheet-pane.active{display:block}
                .sheet-meta{font-size:12px;color:#64748b;margin:0 0 6px}
                .sheet-wrap{overflow:auto;max-height:calc(100vh - 180px);background:#fff;border:1px solid #d8e0ea;border-radius:12px}
                table{border-collapse:collapse;min-width:100%;width:max-content}
                td,th{border-bottom:1px solid #e7edf4;border-right:1px solid #f1f5f9;padding:8px 12px;text-align:left;vertical-align:top;white-space:nowrap;min-width:96px;max-width:360px;overflow:hidden;text-overflow:ellipsis}
                tr:first-child td{position:sticky;top:0;background:#f8fafc;font-weight:600;z-index:1}
                .sheet-img{margin-top:6px}
                .sheet-img img{max-width:180px;max-height:120px;display:block;border:1px solid #e2e8f0;border-radius:6px;cursor:zoom-in}
                .slide-card{background:#fff;border:1px solid #d8e0ea;border-radius:12px;padding:16px;margin-bottom:12px}
                ul{margin:0;padding-left:18px}
                """;
        return "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>"
                + css + "</style></head><body><main class=\"" + pageClass + "\">" + body + "</main></body></html>";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String fixRelativePaths(String htmlContent, String cacheKey) {
        // 如果 HTML 内容已经有完整的 HTML 结构，直接返回
        if (htmlContent.toLowerCase().contains("<html")) {
            return htmlContent;
        }
        // 如果没有完整的 HTML 结构，包装在一个基本的 HTML 页面中
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head><body>" + htmlContent + "</body></html>";
    }

    private record PreviewResolution(String kind, Path path, String contentType, String message, String externalUrl) {
        PreviewResolution(String kind, Path path, String contentType, String message) {
            this(kind, path, contentType, message, null);
        }

        PreviewResolution withExternalUrl(String url) {
            return new PreviewResolution(kind, path, contentType, message, url);
        }
    }
}
