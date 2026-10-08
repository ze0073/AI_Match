package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.Skill;
import com.aimatch.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/tree")
    public Result<?> getSkillTree() {
        return Result.success(skillService.getSkillTree());
    }

    @GetMapping("/category/{category}")
    public Result<?> getByCategory(@PathVariable String category) {
        return Result.success(skillService.getByCategory(category));
    }

    @GetMapping("/search")
    public Result<?> searchSkills(@RequestParam String keyword) {
        return Result.success(skillService.searchSkills(keyword));
    }

    @PostMapping
    public Result<?> addSkill(@RequestBody Skill skill) {
        skillService.addSkill(skill);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result<?> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillService.updateSkill(skill);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return Result.success("删除成功");
    }
}