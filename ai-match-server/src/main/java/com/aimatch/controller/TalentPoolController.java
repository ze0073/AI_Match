package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.Resume;
import com.aimatch.model.entity.User;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "人才库", description = "HR人才库管理：搜索、筛选、收藏候选人")
@RestController
@RequestMapping("/api/hr/talent-pool")
@RequiredArgsConstructor
public class TalentPoolController {

    private final DataStores stores;

    @Operation(summary = "搜索人才库")
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "学历") @RequestParam(required = false) String education,
            @Parameter(description = "工作年限") @RequestParam(required = false) Integer workYears,
            @Parameter(description = "技能") @RequestParam(required = false) String skill,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {

        List<Resume> filtered = stores.resumeStore.findAll(r -> {
            if (r.getStatus() == null || r.getStatus() != 1) return false;
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (r.getName() != null && r.getName().contains(keyword))
                        || (r.getSkills() != null && r.getSkills().contains(keyword))
                        || (r.getJobIntention() != null && r.getJobIntention().contains(keyword))
                        || (r.getCurrentPosition() != null && r.getCurrentPosition().contains(keyword));
                if (!match) return false;
            }
            if (education != null && !education.isEmpty() && r.getEducation() != null && !r.getEducation().contains(education)) return false;
            if (workYears != null && r.getWorkYears() != null && r.getWorkYears() < workYears) return false;
            if (skill != null && !skill.isEmpty() && r.getSkills() != null && !r.getSkills().contains(skill)) return false;
            return true;
        });

        filtered.sort(Comparator.comparing(Resume::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        int total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Resume> pageList = from < total ? filtered.subList(from, to) : List.of();

        List<Map<String, Object>> enrichedRecords = pageList.stream().map(resume -> {
            Map<String, Object> map = new HashMap<>();
            map.put("resumeId", resume.getId());
            map.put("name", resume.getName());
            map.put("education", resume.getEducation());
            map.put("school", resume.getSchool());
            map.put("major", resume.getMajor());
            map.put("workYears", resume.getWorkYears());
            map.put("currentCompany", resume.getCurrentCompany());
            map.put("currentPosition", resume.getCurrentPosition());
            map.put("skills", resume.getSkills());
            map.put("jobIntention", resume.getJobIntention());
            map.put("expectedCity", resume.getExpectedCity());
            map.put("expectedSalary", resume.getExpectedSalary());
            map.put("updateTime", resume.getUpdateTime());

            User user = stores.userStore.findById(resume.getUserId());
            if (user != null) {
                map.put("phone", user.getPhone());
                map.put("email", user.getEmail());
            }

            // Calculate best match score against all active jobs
            List<com.aimatch.model.entity.Job> activeJobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);
            double bestScore = 0;
            String bestJobTitle = null;
            Long bestJobId = null;
            for (com.aimatch.model.entity.Job job : activeJobs) {
                double score = calculateSkillMatch(resume.getSkills(), job.getSkillRequirements());
                if (score > bestScore) {
                    bestScore = score;
                    bestJobTitle = job.getTitle();
                    bestJobId = job.getId();
                }
            }
            map.put("matchScore", Math.round(bestScore * 100));
            map.put("bestMatchJob", bestJobTitle);
            map.put("bestMatchJobId", bestJobId);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }


    private double calculateSkillMatch(String resumeSkills, String jobSkills) {
        if (resumeSkills == null || resumeSkills.isEmpty()) return 0;
        if (jobSkills == null || jobSkills.isEmpty()) return 0;
        try {
            java.util.Set<String> resumeSet = parseSkillSet(resumeSkills);
            java.util.Set<String> jobSet = parseSkillSet(jobSkills);
            if (jobSet.isEmpty()) return 0;
            long matched = resumeSet.stream().filter(jobSet::contains).count();
            return (double) matched / jobSet.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private java.util.Set<String> parseSkillSet(String skillsJson) {
        java.util.Set<String> skills = new java.util.HashSet<>();
        try {
            Object parsed = cn.hutool.json.JSONUtil.parse(skillsJson);
            if (parsed instanceof cn.hutool.json.JSONArray arr) {
                for (Object item : arr) {
                    if (item instanceof cn.hutool.json.JSONObject obj) {
                        String name = obj.getStr("name");
                        if (name != null) skills.add(name.toLowerCase());
                    } else if (item instanceof String s) {
                        skills.add(s.toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            String raw = skillsJson.replaceAll("[\\[\\]\"]", "");
            for (String s : raw.split("[,;]")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) skills.add(trimmed.toLowerCase());
            }
        }
        return skills;
    }
}