package com.aimatch.controller;

import com.aimatch.config.ratelimit.RateLimit;
import com.aimatch.model.Result;
import com.aimatch.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Tag(name = "智能匹配", description = "人才与岗位智能匹配相关接口")
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "求职者匹配岗位", description = "根据简历ID为求职者匹配所有合适的岗位")
    @RateLimit(permitsPerSecond = 5.0 / 60.0)
    @PostMapping("/person-to-jobs")
    public Result<?> matchPersonToJobs(@Parameter(description = "简历ID") @RequestParam Long resumeId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(matchService.matchPersonToJobs(resumeId, userId));
    }

    @Operation(summary = "岗位匹配候选人", description = "根据岗位ID为HR匹配所有合适的候选人")
    @RateLimit(permitsPerSecond = 5.0 / 60.0)
    @PostMapping("/job-to-candidates/{jobId}")
    public Result<?> matchJobToCandidates(@Parameter(description = "岗位ID") @PathVariable Long jobId) {
        return Result.success(matchService.matchJobToCandidates(jobId));
    }

    @Operation(summary = "获取求职者推荐列表")
    @GetMapping("/recommendations")
    public Result<?> getRecommendations(Principal principal,
                                         @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(matchService.getRecommendationsForJobseeker(userId, limit));
    }

    @Operation(summary = "获取岗位推荐候选人列表")
    @GetMapping("/recommendations/job/{jobId}")
    public Result<?> getRecommendationsForJob(@Parameter(description = "岗位ID") @PathVariable Long jobId,
                                               @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(matchService.getRecommendationsForHr(jobId, limit));
    }

    @Operation(summary = "获取匹配详情")
    @GetMapping("/{matchId}")
    public Result<?> getMatchDetail(@Parameter(description = "匹配记录ID") @PathVariable Long matchId) {
        return Result.success(matchService.getMatchDetail(matchId));
    }
}
