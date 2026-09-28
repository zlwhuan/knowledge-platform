package com.company.knowledge.service;

import com.company.knowledge.dto.SkillChatMessageResponse;
import com.company.knowledge.dto.SkillChatSessionResponse;
import com.company.knowledge.entity.SkillChatMessage;
import com.company.knowledge.entity.SkillChatSession;
import com.company.knowledge.entity.SkillSessionAttachment;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.SkillChatMessageRepository;
import com.company.knowledge.repository.SkillChatSessionRepository;
import com.company.knowledge.repository.SkillSessionAttachmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能会话：多用户隔离存储 + 会话内检索缓存
 */
@Service
public class SkillChatSessionService {

    private static final Logger logger = LoggerFactory.getLogger(SkillChatSessionService.class);

    private final SkillChatSessionRepository sessionRepository;
    private final SkillChatMessageRepository messageRepository;
    private final SkillSessionAttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SkillChatSessionService(
            SkillChatSessionRepository sessionRepository,
            SkillChatMessageRepository messageRepository,
            SkillSessionAttachmentRepository attachmentRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
    }

    public List<SkillChatSessionResponse> listByUsername(String username) {
        List<SkillChatSessionResponse> out = new ArrayList<>();
        for (SkillChatSession s : sessionRepository.findByUsernameOrderByUpdatedAtDesc(username)) {
            int count = messageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(s.getId()).size();
            out.add(new SkillChatSessionResponse(s.getId(), s.getSkillId(), s.getTitle(), count, s.getUpdatedAt()));
        }
        return out;
    }

    @Transactional
    public SkillChatSessionResponse create(String username, String skillId, String title) {
        SkillChatSession s = new SkillChatSession();
        s.setUsername(username);
        s.setSkillId(skillId);
        String t = title == null ? "" : title.trim();
        if (t.length() > 10) t = t.substring(0, 10);
        s.setTitle(t.isEmpty() ? "新会话" : t);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        s.setConsumedKeys("[]");
        s.setSourceCache("[]");
        sessionRepository.save(s);
        return new SkillChatSessionResponse(s.getId(), s.getSkillId(), s.getTitle(), 0, s.getUpdatedAt());
    }

    public List<SkillChatMessageResponse> listMessages(String username, Long sessionId) {
        SkillChatSession s = sessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在"));
        List<SkillChatMessageResponse> out = new ArrayList<>();
        for (SkillChatMessage m : messageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(s.getId())) {
            out.add(new SkillChatMessageResponse(
                    m.getId(),
                    m.getRole(),
                    m.getContent(),
                    parseJsonList(m.getSourcesJson()),
                    parseJsonList(m.getTracesJson()),
                    m.getCreatedAt()
            ));
        }
        return out;
    }

    @Transactional
    public void deleteSession(String username, Long sessionId) {
        sessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在"));
        messageRepository.deleteBySessionId(sessionId);
        attachmentRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteByIdAndUsername(sessionId, username);
    }

    /** 保存会话临时附件全文（对话时直接注入，不依赖模型检索） */
    @Transactional
    public Map<String, Object> saveAttachment(Long sessionId, String attachmentId,
                                              String filename, String content,
                                              String filePath, String contentType, Long fileSize) {
        SkillSessionAttachment a = new SkillSessionAttachment();
        a.setSessionId(sessionId);
        a.setAttachmentId(attachmentId);
        a.setFilename(filename == null ? "附件" : filename);
        a.setContent(content == null ? "" : content);
        a.setCharCount(content == null ? 0 : content.length());
        a.setFilePath(filePath);
        a.setContentType(contentType);
        a.setFileSize(fileSize == null ? 0L : fileSize);
        a.setCreatedAt(LocalDateTime.now());
        attachmentRepository.save(a);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("attachmentId", attachmentId);
        m.put("filename", a.getFilename());
        m.put("chars", a.getCharCount());
        return m;
    }

    public List<Map<String, Object>> listAttachments(Long sessionId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SkillSessionAttachment a : attachmentRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getAttachmentId() != null ? a.getAttachmentId() : String.valueOf(a.getId()));
            m.put("filename", a.getFilename());
            m.put("chars", a.getCharCount() == null ? 0 : a.getCharCount());
            m.put("createdAt", a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
            out.add(m);
        }
        return out;
    }

    /** 附件全文（对话注入用） */
    public List<SkillSessionAttachment> listAttachmentEntities(Long sessionId) {
        return attachmentRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public SkillSessionAttachment getAttachment(Long sessionId, String attachmentId) {
        return attachmentRepository.findBySessionIdAndAttachmentId(sessionId, attachmentId).orElse(null);
    }

    @Transactional
    public void deleteAttachment(Long sessionId, String attachmentId) {
        attachmentRepository.deleteBySessionIdAndAttachmentId(sessionId, attachmentId);
    }

    @Transactional
    public void appendUserMessage(Long sessionId, String content) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null) return;
        SkillChatMessage m = new SkillChatMessage();
        m.setSessionId(sessionId);
        m.setRole("user");
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        messageRepository.save(m);
        // 首条消息生成标题（≤10 字）；手动新建的「新会话」也会被覆盖
        if (s.getTitle() == null || s.getTitle().isBlank() || "新会话".equals(s.getTitle())) {
            s.setTitle(autoTitle(content));
        }
        s.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(s);
    }

    /** 从首条提问提取会话标题（最长 10 字） */
    private static String autoTitle(String content) {
        if (content == null || content.isBlank()) return "新会话";
        String t = content.trim().replaceAll("\\s+", " ");
        int cut = t.length();
        for (String sep : new String[]{"。", "？", "?", "！", "!", "；", ";", "\n", "，", ","}) {
            int i = t.indexOf(sep);
            if (i > 0 && i < cut) cut = i;
        }
        String title = t.substring(0, Math.min(cut, 10)).trim();
        return title.isEmpty() ? "新会话" : title;
    }

    @Transactional
    public void updateTitle(Long sessionId, String title) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null) return;
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) return;
        s.setTitle(t.length() > 10 ? t.substring(0, 10) : t);
        s.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(s);
    }

    @Transactional
    public void appendAssistantMessage(Long sessionId, String content,
                                       List<Map<String, Object>> sources,
                                       List<Map<String, Object>> traces) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null) return;
        SkillChatMessage m = new SkillChatMessage();
        m.setSessionId(sessionId);
        m.setRole("assistant");
        m.setContent(content);
        m.setSourcesJson(toJson(sources));
        m.setTracesJson(toJson(traces));
        m.setCreatedAt(LocalDateTime.now());
        messageRepository.save(m);
        s.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(s);
    }

    /** 会话内已用过的检索块 key */
    @SuppressWarnings("unchecked")
    public Set<String> getConsumedKeys(Long sessionId) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null || s.getConsumedKeys() == null || s.getConsumedKeys().isBlank()) {
            return new LinkedHashSet<>();
        }
        try {
            List<String> list = objectMapper.readValue(s.getConsumedKeys(), new TypeReference<List<String>>() {});
            return new LinkedHashSet<>(list);
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }

    @Transactional
    public void saveConsumedKeys(Long sessionId, Set<String> keys) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null) return;
        s.setConsumedKeys(toJson(new ArrayList<>(keys)));
        s.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(s);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSourceCache(Long sessionId) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null || s.getSourceCache() == null || s.getSourceCache().isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(s.getSourceCache(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void saveSourceCache(Long sessionId, List<Map<String, Object>> sources) {
        SkillChatSession s = sessionRepository.findById(sessionId).orElse(null);
        if (s == null) return;
        s.setSourceCache(toJson(sources));
        s.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(s);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o == null ? List.of() : o);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
