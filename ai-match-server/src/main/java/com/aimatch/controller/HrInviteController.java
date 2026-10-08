package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Tag(name = "HR Invite", description = "HR invite candidates for interview")
@RestController
@RequestMapping("/api/hr/invite")
@RequiredArgsConstructor
public class HrInviteController {

    private final DataStores stores;

    @Operation(summary = "Invite a candidate")
    @PostMapping
    public Result<?> inviteCandidate(@RequestBody Map<String, Object> body, Principal principal) {
        Long resumeId = Long.valueOf(body.get("resumeId").toString());
        Long jobId = Long.valueOf(body.get("jobId").toString());

        long exists = stores.matchRecordStore.count(r ->
            r.getResumeId().equals(resumeId) && r.getJobId().equals(jobId) && "hr_invite".equals(r.getMatchType()));
        if (exists > 0) return Result.error("??????");

        Job job = stores.jobStore.findById(jobId);
        Resume resume = stores.resumeStore.findById(resumeId);
        if (job == null || resume == null) return Result.error("???????");

        MatchRecord record = new MatchRecord();
        record.setResumeId(resumeId);
        record.setJobId(jobId);
        record.setUserId(resume.getUserId());
        record.setMatchScore(new java.math.BigDecimal(body.get("matchScore") != null ? body.get("matchScore").toString() : "0"));
        record.setMatchType("hr_invite");
        record.setStatus(0);
        stores.matchRecordStore.save(record);

        Notification notif = new Notification();
        notif.setUserId(resume.getUserId());
        notif.setTitle("面试邀约");
        notif.setContent(job.getCompany() + " 邀请您面试：" + job.getTitle());
        notif.setType("invite");
        notif.setIsRead(0);
        stores.notificationStore.save(notif);

        return Result.success("?????");
    }
}
