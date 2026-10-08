package com.aimatch.service.impl;

import com.aimatch.model.entity.Skill;
import com.aimatch.service.SkillService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final DataStores stores;

    @Override
    public List<Skill> getSkillTree() {
        return stores.skillStore.findAll(s -> s.getStatus() != null && s.getStatus() == 1)
                .stream()
                .sorted(Comparator.comparing(Skill::getLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Skill::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> getByCategory(String category) {
        return stores.skillStore.findAll(s -> category.equals(s.getCategory()) && s.getStatus() != null && s.getStatus() == 1)
                .stream()
                .sorted(Comparator.comparing(Skill::getLevel, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public void addSkill(Skill skill) {
        skill.setStatus(1);
        stores.skillStore.save(skill);
    }

    @Override
    public void updateSkill(Skill skill) {
        stores.skillStore.update(skill);
    }

    @Override
    public List<Skill> searchSkills(String keyword) {
        return stores.skillStore.findAll(s ->
                s.getStatus() != null && s.getStatus() == 1 &&
                s.getName() != null && s.getName().contains(keyword));
    }

    @Override
    public void deleteSkill(Long id) {
        Skill skill = stores.skillStore.findById(id);
        if (skill != null) {
            skill.setStatus(0);
            stores.skillStore.update(skill);
        }
    }
}
