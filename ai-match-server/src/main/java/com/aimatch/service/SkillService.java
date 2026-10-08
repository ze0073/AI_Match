package com.aimatch.service;

import com.aimatch.model.entity.Skill;
import java.util.List;

public interface SkillService {

    List<Skill> getSkillTree();

    List<Skill> getByCategory(String category);

    void addSkill(Skill skill);

    void updateSkill(Skill skill);

    List<Skill> searchSkills(String keyword);

    void deleteSkill(Long id);
}
