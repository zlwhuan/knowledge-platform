package com.company.knowledge.repository;

import com.company.knowledge.entity.SkillChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillChatSessionRepository extends JpaRepository<SkillChatSession, Long> {
    List<SkillChatSession> findByUsernameOrderByUpdatedAtDesc(String username);
    Optional<SkillChatSession> findByIdAndUsername(Long id, String username);
    void deleteByIdAndUsername(Long id, String username);
}
