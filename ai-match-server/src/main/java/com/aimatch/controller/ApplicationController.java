package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.model.Result;
import com.aimatch.model.entity.Job;
import com.aimatch.model.entity.MatchRecord;
import com.aimatch.model.entity.Notification;
import com.aimatch.model.entity.Resume;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Job Applications", description = "Jobseeker apply for jobs and manage applications")
@RestController
@RequestMapping("/api/jobseeker/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final DataStores stores;

    @Operation(summary = "Apply for job (JSON body)")
    @PostMapping
    public Result<?> applyByBody(@RequestBody Map<String, Object> body, Principal principal) {
        Long resumeId = body.get("resumeId") != null ? Long.valueOf(body.get("resumeId").toString()) : null;
        Long jobId = body.get("jobId") != null ? Long.valueOf(body.get("jobId").toString()) : null;
        return doApply(resumeId, jobId, principal);
    }

    @Operation(summary = "Apply for job (params)")
    @PostMapping("/apply")
    public Result<?> applyByParams(@RequestParam Long resumeId, @RequestParam Long jobId, Principal principal) {
        return doApply(resumeId, jobId, principal);
    }

    private Result<?> doApply(Long resumeId, Long jobId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        if (resumeId == null) {
            List<Resume> resumes = stores.resumeStore.findAll(
                r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
            if (resumes.isEmpty()) return Result.error("请先上传简历");
            resumeId = resumes.get(0).getId();
        }

        final Long finalResumeId = resumeId;
        long existing = stores.matchRecordStore.count(r ->
            r.getResumeId().equals(finalResumeId) &&
            r.getJobId().equals(jobId) &&
            r.getUserId().equals(userId) &&
            ("manual_apply".equals(r.getMatchType()) || "hr_invite".equals(r.getMatchType()) || "PERSON_TO_JOB".equals(r.getMatchType()))
        );
        if (existing > 0) {
            return Result.error("您已投递过该岗位，请勿重复投递");
        }

        MatchRecord record = new MatchRecord();
        record.setResumeId(resumeId);
        record.setJobId(jobId);
        record.setUserId(userId);
        record.setMatchScore(new java.math.BigDecimal("0"));
        record.setMatchType("manual_apply");
        record.setStatus(1);
        stores.matchRecordStore.save(record);

        try {
            Job job = stores.jobStore.findById(jobId);
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setTitle("投递成功");
            notif.setContent("您已成功投递职位：" + (job != null ? job.getTitle() : "未知职位"));
            notif.setType("投递");
            notif.setIsRead(0);
            stores.notificationStore.save(notif);
        } catch (Exception ignored) {}

        return Result.success("????");
    }

    @Operation(summary = "Get my applications")
    @GetMapping
    public Result<List<Map<String, Object>>> getMyApplications(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        List<MatchRecord> records = stores.matchRecordStore.findAll(r ->
            r.getUserId().equals(userId) &&
            ("manual_apply".equals(r.getMatchType()) || "hr_invite".equals(r.getMatchType()) || "PERSON_TO_JOB".equals(r.getMatchType()))
        );
        records.sort(Comparator.comparing(MatchRecord::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("matchId", r.getId());
            map.put("resumeId", r.getResumeId());
            map.put("jobId", r.getJobId());
            map.put("matchScore", r.getMatchScore());
            map.put("status", r.getStatus() != null ? r.getStatus() : 0);
            map.put("createTime", r.getCreateTime());

            Job job = stores.jobStore.findById(r.getJobId());
            if (job != null) {
                map.put("jobTitle", job.getTitle());
                map.put("company", job.getCompany());
                map.put("city", job.getCity());
                map.put("salary", job.getSalary());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Operation(summary = "Update application status")
    @PutMapping("/{matchId}/status")
    public Result<?> updateStatus(
            @Parameter(description = "Match record ID") @PathVariable Long matchId,
            @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        MatchRecord record = stores.matchRecordStore.findById(matchId);
        if (record == null) return Result.error("?????");
        record.setStatus(status);
        stores.matchRecordStore.update(record);
        return Result.success("?????");
    }

    @Operation(summary = "Withdraw application")
    @DeleteMapping("/{matchId}")
    public Result<?> withdraw(
            @Parameter(description = "Match record ID") @PathVariable Long matchId,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        MatchRecord record = stores.matchRecordStore.findById(matchId);
        if (record == null) return Result.error("?????");
        if (!record.getUserId().equals(userId)) return Result.error("???");
        if (record.getStatus() != null && record.getStatus() != 1) return Result.error("?????????");
        stores.matchRecordStore.deleteById(matchId);
        return Result.success("?????");
    }
}
