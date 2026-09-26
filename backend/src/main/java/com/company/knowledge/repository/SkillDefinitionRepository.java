package com.company.knowledge.repository;

import com.company.knowledge.entity.SkillDefinitionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinitionEntity, Long> {
    Optional<SkillDefinitionEntity> findBySkillKey(String skillKey);
    List<SkillDefinitionEntity> findAllByOrderBySortOrderAscIdAsc();
    boolean existsBySkillKey(String skillKey);
}
