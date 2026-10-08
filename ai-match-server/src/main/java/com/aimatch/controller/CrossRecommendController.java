package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "跨职位推荐", description = "从人才库为新职位智能推荐候选人")
@RestController
@RequestMapping("/api/hr/cross-recommend")
@RequiredArgsConstructor
public class CrossRecommendController {

    private final DataStores stores;

    @Operation(summary = "从人才库为指定职位推荐候选人")
    @GetMapping("/{jobId}")
    public Result<List<Map<String, Object>>> recommendForJob(
            @PathVariable Long jobId, Principal principal) {
        Long hrId = Long.parseLong(principal.getName());

        Job job = stores.jobStore.findById(jobId);
        if (job == null) return Result.error("职位不存在");

        List<TalentFavorite> favorites = stores.talentFavoriteStore.findAll(f -> f.getHrId().equals(hrId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TalentFavorite fav : favorites) {
            Resume resume = stores.resumeStore.findById(fav.getResumeId());
            if (resume == null || resume.getStatus() != 1) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("favoriteId", fav.getId());
            map.put("resumeId", resume.getId());
            map.put("name", resume.getName());
            map.put("education", resume.getEducation());
            map.put("workYears", resume.getWorkYears());
            map.put("currentPosition", resume.getCurrentPosition());
            map.put("skills", skillsToList(resume.getSkills()));
            map.put("expectedCity", resume.getExpectedCity());
            map.put("category", fav.getCategory());

            int score = 50;
            try {
                List<String> resumeSkills = skillsToList(resume.getSkills());
                List<String> jobSkills = skillsToList(job.getSkillRequirements());
                if (!resumeSkills.isEmpty() && !jobSkills.isEmpty()) {
                    long match = resumeSkills.stream().filter(s -> jobSkills.stream().anyMatch(js -> js.contains(s) || s.contains(js))).count();
                    score += (int)(match * 40.0 / Math.max(jobSkills.size(), 1));
                }
            } catch (Exception e) {}
            if (job.getEducation() != null && resume.getEducation() != null && resume.getEducation().contains(job.getEducation())) {
                score += 15;
            }
            if (resume.getExpectedCity() != null && job.getCity() != null && resume.getExpectedCity().contains(job.getCity())) {
                score += 10;
            }
            map.put("matchScore", Math.min(score, 99));
            result.add(map);
        }

        result.sort((a, b) -> Integer.compare((int)b.get("matchScore"), (int)a.get("matchScore")));
        return Result.success(result);
    }

    private List<String> skillsToList(String skills) {
        if (skills == null || skills.isEmpty()) return List.of();
        try { return JSONUtil.parseArray(skills).toList(String.class); }
        catch (Exception e) { return Arrays.asList(skills.split("[,，、]")); }
    }
}
