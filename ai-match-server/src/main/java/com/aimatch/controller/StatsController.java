package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "数据统计", description = "管理后台数据统计接口")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DataStores stores;

    @Operation(summary = "获取Dashboard统计数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", stores.userStore.count());
        stats.put("resumeCount", stores.resumeStore.count());
        stats.put("jobCount", stores.jobStore.count());
        stats.put("matchCount", stores.matchRecordStore.count());
        return Result.success(stats);
    }

    @Operation(summary = "获取HR招聘漏斗数据")
    @GetMapping("/funnel")
    public Result<Map<String, Integer>> getFunnelData() {
        Map<String, Integer> funnel = new HashMap<>();
        funnel.put("applied", (int) stores.matchRecordStore.count(r -> r.getStatus() != null && r.getStatus() == 1));
        funnel.put("interviewing", (int) stores.matchRecordStore.count(r -> r.getStatus() != null && r.getStatus() == 2));
        funnel.put("hired", (int) stores.matchRecordStore.count(r -> r.getStatus() != null && r.getStatus() == 3));
        funnel.put("rejected", (int) stores.matchRecordStore.count(r -> r.getStatus() != null && r.getStatus() == 4));
        return Result.success(funnel);
    }

    @Operation(summary = "获取简历总数")
    @GetMapping("/resume-count")
    public Result<Long> getResumeCount() {
        return Result.success(stores.resumeStore.count(r -> r.getStatus() != null && r.getStatus() == 1));
    }

    @Operation(summary = "获取系统运行状态")
    @GetMapping("/system")
    public Result<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        stats.put("cpuCores", runtime.availableProcessors());
        stats.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        stats.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        stats.put("onlineUsers", stores.userStore.count(u -> u.getStatus() != null && u.getStatus() == 1));
        stats.put("totalResumes", stores.resumeStore.count());
        stats.put("totalJobs", stores.jobStore.count());
        stats.put("totalMatches", stores.matchRecordStore.count());
        return Result.success(stats);
    }
}
