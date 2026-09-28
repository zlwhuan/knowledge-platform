package com.company.knowledge.repository;

import com.company.knowledge.entity.SkillSessionAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillSessionAttachmentRepository extends JpaRepository<SkillSessionAttachment, Long> {
    List<SkillSessionAttachment> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    Optional<SkillSessionAttachment> findBySessionIdAndAttachmentId(Long sessionId, String attachmentId);
    void deleteBySessionId(Long sessionId);
    void deleteBySessionIdAndAttachmentId(Long sessionId, String attachmentId);
}
