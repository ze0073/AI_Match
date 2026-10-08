package com.aimatch.controller;

import com.aimatch.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.aimatch.service.CapabilityGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Tag(name = "能力图谱", description = "人才与岗位能力图谱可视化")
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class CapabilityGraphController {

    private final CapabilityGraphService capabilityGraphService;

    @Operation(summary = "构建个人能力图谱")
    @PostMapping("/build/person")
    public Result<?> buildPersonGraph(@Parameter(description = "简历ID") @RequestParam Long resumeId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(capabilityGraphService.buildPersonCapabilityGraph(userId, resumeId));
    }

    @Operation(summary = "构建职位能力图谱")
    @PostMapping("/build/job/{jobId}")
    public Result<?> buildJobGraph(@Parameter(description = "岗位ID") @PathVariable Long jobId) {
        return Result.success(capabilityGraphService.buildJobCapabilityGraph(jobId));
    }

    @Operation(summary = "获取全局能力图谱数据")
    @GetMapping("/data")
    public Result<?> getGraphData() {
        return Result.success(capabilityGraphService.getCapabilityGraphData());
    }

    @Operation(summary = "获取我的能力图谱")
    @GetMapping("/my")
    public Result<?> getMyGraph(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(capabilityGraphService.getMyGraphData(userId));
    }

    @Operation(summary = "获取技能差距图谱")
    @GetMapping("/gap")
    public Result<?> getGapGraph(@Parameter(description = "简历ID") @RequestParam Long resumeId,
                                  @Parameter(description = "岗位ID") @RequestParam Long jobId) {
        return Result.success(capabilityGraphService.getGapGraph(resumeId, jobId));
    }
}