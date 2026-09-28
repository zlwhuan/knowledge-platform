package com.company.knowledge.repository;

import com.company.knowledge.entity.SkillChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillChatMessageRepository extends JpaRepository<SkillChatMessage, Long> {
    List<SkillChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
