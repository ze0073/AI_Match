package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.model.Result;
import com.aimatch.model.entity.Resume;
import com.aimatch.service.JobService;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "职位管理", description = "职位JD上传、AI解析、搜索管理")
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final DataStores stores;

    @Operation(summary = "上传并AI解析职位JD")
    @PostMapping("/upload")
    public Result<?> uploadJob(@Parameter(description = "JD文件") @RequestParam("file") MultipartFile file, Principal principal) {
        Long hrId = Long.parseLong(principal.getName());
        return Result.success(jobService.uploadAndParse(file, hrId));
    }

    @Operation(summary = "搜索职位(含匹配度)")
    @GetMapping("/search-with-score")
    public Result<?> searchJobsWithScore(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "行业") @RequestParam(required = false) String industry,
            @Parameter(description = "城市") @RequestParam(required = false) String city,
            @Parameter(description = "学历要求") @RequestParam(required = false) String education,
            @Parameter(description = "工作年限") @RequestParam(required = false) Integer workYears,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        var pageResult = jobService.searchJobs(keyword, industry, city, education, workYears, page, size);

        Long userId = Long.parseLong(principal.getName());
        List<String> mySkills = getUserSkills(userId);

        @SuppressWarnings("unchecked")
        var records = (List<Map<String, Object>>) pageResult.get("records");
        var enrichedRecords = records.stream().map(job -> {
            Map<String, Object> map = new HashMap<>(job);
            map.put("matchScore", calcMatchScore(mySkills, (String) job.get("skillRequirements")));
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", pageResult.get("total"));
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @Operation(summary = "搜索职位")
    @GetMapping("/search")
    public Result<?> searchJobs(@Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                 @Parameter(description = "行业") @RequestParam(required = false) String industry,
                                 @Parameter(description = "城市") @RequestParam(required = false) String city,
                                 @Parameter(description = "学历要求") @RequestParam(required = false) String education,
                                 @Parameter(description = "工作年限") @RequestParam(required = false) Integer workYears,
                                 @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                 @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return Result.success(jobService.searchJobs(keyword, industry, city, education, workYears, page, size));
    }

    @Operation(summary = "获取职位详情")
    @GetMapping("/{jobId}")
    public Result<?> getJob(@Parameter(description = "职位ID") @PathVariable Long jobId) {
        return Result.success(jobService.getJobDetail(jobId));
    }

    @Operation(summary = "获取职位能力图谱")
    @GetMapping("/{jobId}/capability")
    public Result<?> getJobCapability(@Parameter(description = "职位ID") @PathVariable Long jobId) {
        return Result.success(jobService.getJobCapability(jobId));
    }

    @Operation(summary = "删除职位")
    @DeleteMapping("/{jobId}")
    public Result<?> deleteJob(@Parameter(description = "职位ID") @PathVariable Long jobId) {
        jobService.deleteJob(jobId);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新职位信息")
    @PutMapping("/{jobId}")
    public Result<?> updateJob(@Parameter(description = "职位ID") @PathVariable Long jobId, @RequestBody Map<String, Object> data) {
        jobService.updateJob(jobId, data);
        return Result.success("更新成功");
    }

    @Operation(summary = "获取相似职位推荐")
    @GetMapping("/{jobId}/similar")
    public Result<?> getSimilarJobs(@Parameter(description = "职位ID") @PathVariable Long jobId) {
        var currentJob = stores.jobStore.findById(jobId);
        if (currentJob == null) return Result.error("Job not found");

        List<String> jobSkills = parseSkillsFromJob(currentJob);
        if (jobSkills.isEmpty()) return Result.success(List.of());

        var allJobs = stores.jobStore.findAll(j ->
            j.getStatus() != null && j.getStatus() == 1 &&
            (currentJob.getIndustry() == null || currentJob.getIndustry().equals(j.getIndustry()) ||
             currentJob.getCity() == null || currentJob.getCity().equals(j.getCity()))
        );
        var similar = allJobs.stream()
                .filter(j -> !j.getId().equals(jobId))
                .map(j -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", j.getId());
                    map.put("title", j.getTitle());
                    map.put("company", j.getCompany());
                    map.put("city", j.getCity());
                    map.put("salary", j.getSalary());
                    map.put("education", j.getEducation());
                    List<String> otherSkills = parseSkillsFromJob(j);
                    long overlap = jobSkills.stream().filter(js ->
                            otherSkills.stream().anyMatch(os ->
                                    os.toLowerCase().contains(js.toLowerCase()) || js.toLowerCase().contains(os.toLowerCase())))
                            .count();
                    map.put("overlapCount", (int) overlap);
                    map.put("matchScore", otherSkills.isEmpty() ? 0 :
                            (int) Math.min(99, overlap * 100.0 / Math.max(jobSkills.size(), otherSkills.size())));
                    return map;
                })
                .filter(m -> (int) m.get("overlapCount") > 0)
                .sorted((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")))
                .limit(5)
                .collect(Collectors.toList());
        return Result.success(similar);
    }

    private List<String> getUserSkills(Long userId) {
        try {
            var resumes = stores.resumeStore.findAll(
                r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
            if (!resumes.isEmpty() && resumes.get(0).getSkills() != null) {
                return JSONUtil.parseArray(resumes.get(0).getSkills()).toList(String.class);
            }
        } catch (Exception e) {}
        return List.of();
    }

    private int calcMatchScore(List<String> mySkills, String skillRequirements) {
        if (mySkills.isEmpty() || skillRequirements == null) return 0;
        try {
            List<String> jobSkills = JSONUtil.parseArray(skillRequirements).toList(String.class);
            if (jobSkills.isEmpty()) return 0;
            long matchCount = mySkills.stream()
                .filter(ms -> jobSkills.stream().anyMatch(js ->
                    js.toLowerCase().contains(ms.toLowerCase()) || ms.toLowerCase().contains(js.toLowerCase())))
                .count();
            return (int) Math.min(99, matchCount * 100.0 / jobSkills.size());
        } catch (Exception e) { return 0; }
    }

    private List<String> parseSkillsFromJob(com.aimatch.model.entity.Job job) {
        try {
            if (job.getSkillRequirements() != null) {
                return JSONUtil.parseArray(job.getSkillRequirements()).toList(String.class);
            }
        } catch (Exception ignored) {}
        return List.of();
    }
}
