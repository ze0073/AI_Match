package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "????", description = "???????????????")
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final DataStores stores;

    @Operation(summary = "??????????")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // Basic counts
        long totalUsers = stores.userStore.count();
        long totalJobs = stores.jobStore.count(j -> j.getStatus() != null && j.getStatus() == 1);
        long totalResumes = stores.resumeStore.count(r -> r.getStatus() != null && r.getStatus() == 1);
        long totalMatches = stores.matchRecordStore.count();

        overview.put("totalUsers", totalUsers);
        overview.put("totalJobs", totalJobs);
        overview.put("totalResumes", totalResumes);
        overview.put("totalMatches", totalMatches);

        // User role distribution
        long jobseekers = stores.userStore.count(u -> "JOBSEEKER".equals(u.getUserType()));
        long hrs = stores.userStore.count(u -> "HR".equals(u.getUserType()));
        long admins = stores.userStore.count(u -> "ADMIN".equals(u.getUserType()));
        overview.put("jobseekers", jobseekers);
        overview.put("hrs", hrs);
        overview.put("admins", admins);

        // Match statistics
        List<MatchRecord> allMatches = stores.matchRecordStore.findAll(m -> true);
        double avgScore = allMatches.stream()
                .filter(m -> m.getMatchScore() != null)
                .mapToDouble(m -> m.getMatchScore().doubleValue())
                .average().orElse(0);
        long hiredCount = allMatches.stream().filter(m -> m.getStatus() != null && m.getStatus() == 3).count();
        long interviewingCount = allMatches.stream().filter(m -> m.getStatus() != null && m.getStatus() == 2).count();
        overview.put("avgMatchScore", Math.round(avgScore * 10.0) / 10.0);
        overview.put("hiredCount", hiredCount);
        overview.put("interviewingCount", interviewingCount);

        return Result.success(overview);
    }

    @Operation(summary = "????????")
    @GetMapping("/city-distribution")
    public Result<List<Map<String, Object>>> getCityDistribution() {
        List<Job> activeJobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);
        Map<String, Long> cityCount = activeJobs.stream()
                .filter(j -> j.getCity() != null && !j.getCity().isEmpty())
                .collect(Collectors.groupingBy(Job::getCity, Collectors.counting()));

        List<Map<String, Object>> result = cityCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "?????????")
    @GetMapping("/education-distribution")
    public Result<List<Map<String, Object>>> getEducationDistribution() {
        List<Resume> activeResumes = stores.resumeStore.findAll(r -> r.getStatus() != null && r.getStatus() == 1);
        Map<String, Long> eduCount = activeResumes.stream()
                .filter(r -> r.getEducation() != null && !r.getEducation().isEmpty())
                .collect(Collectors.groupingBy(Resume::getEducation, Collectors.counting()));

        List<Map<String, Object>> result = eduCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "??????????")
    @GetMapping("/skill-ranking")
    public Result<List<Map<String, Object>>> getSkillRanking() {
        List<Job> activeJobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);
        Map<String, Long> skillCount = new HashMap<>();

        for (Job job : activeJobs) {
            List<String> skills = parseSkills(job.getSkillRequirements());
            for (String skill : skills) {
                skillCount.merge(skill, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = skillCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "????????")
    @GetMapping("/salary-distribution")
    public Result<List<Map<String, Object>>> getSalaryDistribution() {
        List<Job> activeJobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);
        Map<String, Long> salaryRanges = new LinkedHashMap<>();
        salaryRanges.put("10K\u4ee5\u4e0b", 0L);
        salaryRanges.put("10K-20K", 0L);
        salaryRanges.put("20K-30K", 0L);
        salaryRanges.put("30K-50K", 0L);
        salaryRanges.put("50K\u4ee5\u4e0a", 0L);

        for (Job job : activeJobs) {
            if (job.getSalary() == null) continue;
            String range = categorizeSalary(job.getSalary());
            salaryRanges.merge(range, 1L, Long::sum);
        }

        List<Map<String, Object>> result = salaryRanges.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "???????????")
    @GetMapping("/application-trends")
    public Result<List<Map<String, Object>>> getApplicationTrends() {
        List<MatchRecord> records = stores.matchRecordStore.findAll(r ->
                "manual_apply".equals(r.getMatchType()) || "hr_invite".equals(r.getMatchType()) || "PERSON_TO_JOB".equals(r.getMatchType()));

        Map<String, Long> dateCount = new LinkedHashMap<>();
        for (MatchRecord r : records) {
            if (r.getCreateTime() == null) continue;
            String date = r.getCreateTime().toLocalDate().toString();
            dateCount.merge(date, 1L, Long::sum);
        }

        List<Map<String, Object>> result = dateCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    private List<String> parseSkills(String skillJson) {
        if (skillJson == null || skillJson.isEmpty()) return List.of();
        try {
            cn.hutool.json.JSONArray arr = JSONUtil.parseArray(skillJson);
            List<String> result = new ArrayList<>();
            for (Object item : arr) {
                if (item instanceof String) {
                    result.add((String) item);
                } else if (item instanceof cn.hutool.json.JSONObject) {
                    String name = ((cn.hutool.json.JSONObject) item).getStr("name");
                    if (name != null && !name.isEmpty()) result.add(name);
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String categorizeSalary(String salary) {
        try {
            // Handle range like "15K-25K" or single like "25K"
            String numStr = salary.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) return "10K以下";
            // Take the first number (lower bound of range)
            // For "1525" from "15K-25K", extract "15"
            int num;
            if (numStr.length() >= 4) {
                num = Integer.parseInt(numStr.substring(0, 2));
            } else if (numStr.length() >= 2) {
                num = Integer.parseInt(numStr.substring(0, 1));
            } else {
                num = Integer.parseInt(numStr);
            }
            if (num <= 10) return "10K以下";
            if (num <= 20) return "10K-20K";
            if (num <= 30) return "20K-30K";
            if (num <= 50) return "30K-50K";
            return "50K以上";
        } catch (Exception e) {
            return "10K以下";
        }
    }
}
