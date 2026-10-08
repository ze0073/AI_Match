package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.ai.AiParseService;
import com.aimatch.model.Result;
import com.aimatch.model.entity.Job;
import com.aimatch.model.entity.Resume;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Tag(name = "AI面试准备", description = "AI生成面试题目和回答评估")
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final AiParseService aiParseService;
    private final DataStores stores;

    @Operation(summary = "根据职位生成面试题")
    @GetMapping("/questions/{jobId}")
    public Result<?> getQuestions(@PathVariable Long jobId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        Job job = stores.jobStore.findById(jobId);
        if (job == null) return Result.error("职位不存在");

        String jobSkills = job.getSkillRequirements() != null ? job.getSkillRequirements() : "[]";

        String resumeSkills = "[]";
        try {
            List<Resume> resumes = stores.resumeStore.findAll(
                r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
            resumes.sort(Comparator.comparing(Resume::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
            if (!resumes.isEmpty() && resumes.get(0).getSkills() != null) {
                resumeSkills = resumes.get(0).getSkills();
            }
        } catch (Exception ignored) {}

        try {
            String result = aiParseService.generateInterviewQuestions(
                job.getTitle(), jobSkills, resumeSkills);
            List<?> questions = JSONUtil.parseArray(result);
            return Result.success(questions);
        } catch (Exception e) {
            return Result.error("AI服务暂时不可用: " + e.getMessage());
        }
    }

    @Operation(summary = "评估面试回答")
    @PostMapping("/evaluate")
    public Result<?> evaluateAnswer(@RequestBody Map<String, String> body, Principal principal) {
        String question = body.get("question");
        String answer = body.get("answer");
        String jobTitle = body.getOrDefault("jobTitle", "");

        if (question == null || answer == null || answer.trim().isEmpty()) {
            return Result.error("题目和回答不能为空");
        }

        try {
            String result = aiParseService.evaluateInterviewAnswer(question, answer, jobTitle);
            return Result.success(JSONUtil.parseObj(result));
        } catch (Exception e) {
            return Result.error("AI评估失败: " + e.getMessage());
        }
    }

    @Operation(summary = "生成学习路线")
    @GetMapping("/roadmap/{jobId}")
    public Result<?> getRoadmap(@PathVariable Long jobId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        Job job = stores.jobStore.findById(jobId);
        if (job == null) return Result.error("职位不存在");

        String jobSkills = job.getSkillRequirements() != null ? job.getSkillRequirements() : "[]";

        String resumeSkills = "[]";
        try {
            List<Resume> resumes = stores.resumeStore.findAll(
                r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
            resumes.sort(Comparator.comparing(Resume::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
            if (!resumes.isEmpty() && resumes.get(0).getSkills() != null) {
                resumeSkills = resumes.get(0).getSkills();
            }
        } catch (Exception ignored) {}

        try {
            String result = aiParseService.generateLearningRoadmap(
                job.getTitle(), jobSkills, resumeSkills);
            return Result.success(JSONUtil.parseObj(result));
        } catch (Exception e) {
            return Result.error("AI服务暂时不可用: " + e.getMessage());
        }
    }
}
