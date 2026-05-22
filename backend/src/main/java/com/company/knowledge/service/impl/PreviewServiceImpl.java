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
import java.util.List;
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
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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

    private PreviewResolution resolvePreview(Attachment attachment) {
        String kind = detectKind(attachment.getOriginalFileName(), attachment.getContentType());
        Path source = Paths.get(attachment.getFilePath());

        if ("office-word".equals(kind) || "office-sheet".equals(kind) || "office-slide".equals(kind)) {
            Path pdf = convertOfficeToPdf(source, attachment.getId());
            if (pdf != null) {
                return new PreviewResolution(
                        "pdf",
                        pdf,
                        "application/pdf",
                        "已通过 LibreOffice 离线转换为 PDF 预览，尽可能还原原始版式"
                );
            }

            String onlyOfficeUrl = buildOnlyOfficeUrl(attachment);
            if (onlyOfficeUrl != null) {
                return new PreviewResolution(
                        "onlyoffice",
                        null,
                        "text/html; charset=UTF-8",
                        "LibreOffice 转换失败，已回退为 OnlyOffice 文档预览"
                ).withExternalUrl(onlyOfficeUrl);
            }

            Path html = buildHtmlPreview(source, kind, attachment.getId());
            return new PreviewResolution(
                    kind,
                    html,
                    "text/html; charset=UTF-8",
                    "LibreOffice/OnlyOffice 均不可用，已降级为结构化文本预览"
            );
        }

        if ("markdown".equals(kind) || "html".equals(kind) || "text".equals(kind)) {
            return new PreviewResolution(kind, buildHtmlPreview(source, kind, attachment.getId()), "text/html; charset=UTF-8", "已生成离线预览页面");
        }

        if ("image".equals(kind) || "video".equals(kind) || "audio".equals(kind) || "pdf".equals(kind)) {
            String type = (attachment.getContentType() == null || attachment.getContentType().isBlank()) ? "application/octet-stream" : attachment.getContentType();
            return new PreviewResolution(kind, source, type, "原始文件可直接离线预览");
        }

        return new PreviewResolution("fallback", source, "application/octet-stream", "该文件暂不支持结构化预览，可直接下载查看");
    }

    private Path buildHtmlPreview(Path source, String kind, Long attachmentId) {
        try {
            Path dir = previewRoot.resolve("attachment-" + attachmentId);
            Files.createDirectories(dir);
            Path target = dir.resolve("preview.html");
            if (Files.exists(target) && Files.getLastModifiedTime(target).toMillis() >= Files.getLastModifiedTime(source).toMillis()) {
                return target;
            }

            switch (kind) {
                case "office-word" -> Files.writeString(target, wrapHtml(extractWord(source), false), StandardCharsets.UTF_8);
                case "office-sheet" -> Files.writeString(target, wrapHtml(extractSheet(source), true), StandardCharsets.UTF_8);
                case "office-slide" -> Files.writeString(target, wrapHtml(extractSlide(source), false), StandardCharsets.UTF_8);
                case "html" -> {
                    String htmlContent = Files.readString(source, StandardCharsets.UTF_8);
                    Files.writeString(target, htmlContent, StandardCharsets.UTF_8);
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

    private Path convertOfficeToPdf(Path source, Long attachmentId) {
        try {
            Path dir = previewRoot.resolve("attachment-" + attachmentId);
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
                        "pdf:writer_pdf_Export",
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

    private String extractWord(Path source) throws IOException {
        String lower = source.getFileName().toString().toLowerCase();
        if (lower.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(source); XWPFDocument document = new XWPFDocument(in); XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return renderPlainText(extractor.getText());
            }
        }
        try (InputStream in = Files.newInputStream(source); HWPFDocument document = new HWPFDocument(in); WordExtractor extractor = new WordExtractor(document)) {
            return renderPlainText(extractor.getText());
        }
    }

    private String extractSheet(Path source) throws IOException {
        try (InputStream in = Files.newInputStream(source); Workbook workbook = WorkbookFactory.create(in)) {
            DataFormatter formatter = new DataFormatter();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                var sheet = workbook.getSheetAt(i);
                builder.append("<section><h2>").append(escape(sheet.getSheetName())).append("</h2><div class=\"sheet-wrap\"><table><tbody>");
                for (Row row : sheet) {
                    builder.append("<tr>");
                    row.forEach(cell -> builder.append("<td>").append(escape(formatter.formatCellValue(cell))).append("</td>"));
                    builder.append("</tr>");
                }
                builder.append("</tbody></table></div></section>");
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
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
        return "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>body{margin:0;background:#eef2f6;font-family:Segoe UI,PingFang SC,Microsoft YaHei,sans-serif;color:#1f2937}.preview-page{padding:24px;line-height:1.75}.preview-page pre{white-space:pre-wrap;background:#fff;border:1px solid #d8e0ea;border-radius:16px;padding:18px;box-shadow:0 10px 24px rgba(15,23,42,.05)}.preview-page h2{margin:0 0 14px}.sheet-wrap{overflow:auto;background:#fff;border:1px solid #d8e0ea;border-radius:16px;box-shadow:0 10px 24px rgba(15,23,42,.05)}table{width:100%;border-collapse:collapse}td,th{border-bottom:1px solid #e7edf4;padding:10px 12px;text-align:left;vertical-align:top}.slide-card{background:#fff;border:1px solid #d8e0ea;border-radius:16px;padding:18px;margin-bottom:16px;box-shadow:0 10px 24px rgba(15,23,42,.05)}ul{margin:0;padding-left:20px}</style></head><body><main class=\"" + pageClass + "\">" + body + "</main></body></html>";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
