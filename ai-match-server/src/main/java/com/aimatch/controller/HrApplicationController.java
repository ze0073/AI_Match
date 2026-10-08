package com.aimatch.controller;

import cn.hutool.json.JSONUtil;
import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "HR Applications", description = "HR view job applications from candidates")
@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class HrApplicationController {

    private final DataStores stores;

    @Operation(summary = "Get applications for HR jobs")
    @GetMapping
    public Result<List<Map<String, Object>>> getApplications(Principal principal) {
        Long hrId = Long.parseLong(principal.getName());

        List<Job> hrJobs = stores.jobStore.findAll(j -> j.getHrId().equals(hrId));
        List<Long> hrJobIds = hrJobs.stream().map(Job::getId).collect(Collectors.toList());

        List<MatchRecord> records = stores.matchRecordStore.findAll(r ->
            hrJobIds.contains(r.getJobId()) && ("manual_apply".equals(r.getMatchType()) || "hr_invite".equals(r.getMatchType())));
        records.sort(Comparator.comparing(MatchRecord::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (MatchRecord r : records) {
            Job job = stores.jobStore.findById(r.getJobId());
            Resume resume = stores.resumeStore.findById(r.getResumeId());
            User user = resume != null ? stores.userStore.findById(resume.getUserId()) : null;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("matchId", r.getId());
            map.put("resumeId", r.getResumeId());
            map.put("jobId", r.getJobId());
            map.put("matchScore", r.getMatchScore());
            map.put("status", r.getStatus() != null ? r.getStatus() : 0);
            map.put("createTime", r.getCreateTime());
            if (job != null) {
                map.put("jobTitle", job.getTitle());
                map.put("company", job.getCompany());
            }
            if (resume != null) {
                map.put("name", resume.getName());
                map.put("currentPosition", resume.getCurrentPosition());
                map.put("education", resume.getEducation());
                map.put("workYears", resume.getWorkYears());
                map.put("skills", resume.getSkills());
            }
            if (user != null) {
                map.put("phone", user.getPhone());
                map.put("email", user.getEmail());
            }
            result.add(map);
        }

        return Result.success(result);
    }

    @Operation(summary = "Update application status")
    @PutMapping("/{matchId}/status")
    public Result<?> updateStatus(@PathVariable Long matchId, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        MatchRecord record = stores.matchRecordStore.findById(matchId);
        if (record == null) return Result.error("?????");
        record.setStatus(status);
        stores.matchRecordStore.update(record);

        try {
            String[] statusNames = {"", "待处理", "面试中", "已录用", "已拒绝"};
            String statusName = status != null && status < statusNames.length ? statusNames[status] : "???";
            Job job = stores.jobStore.findById(record.getJobId());
            Notification notif = new Notification();
            notif.setUserId(record.getUserId());
            notif.setTitle("申请状态更新");
            notif.setContent("您的投递状态已更新为：" + statusName + (job != null ? " (" + job.getTitle() + ")" : ""));
            notif.setType("status");
            notif.setIsRead(0);
            stores.notificationStore.save(notif);
        } catch (Exception ignored) {}

        return Result.success("?????");
    }

    @Operation(summary = "Delete application record")
    @DeleteMapping("/{matchId}")
    public Result<?> deleteApplication(@PathVariable Long matchId) {
        MatchRecord record = stores.matchRecordStore.findById(matchId);
        if (record == null) return Result.error("?????");
        stores.matchRecordStore.deleteById(matchId);
        return Result.success("???");
    }
}