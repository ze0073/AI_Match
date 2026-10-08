package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "系统配置", description = "系统管理员配置管理")
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final DataStores stores;

    @Value("${ai.openai.model:deepseek-chat}")
    private String aiModel;

    @Value("${ai.openai.base-url:https://api.deepseek.com}")
    private String aiBaseUrl;

    @Operation(summary = "获取系统配置和状态")
    @GetMapping
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("aiModel", aiModel);
        config.put("aiBaseUrl", aiBaseUrl);
        config.put("totalUsers", stores.userStore.count());
        config.put("totalResumes", stores.resumeStore.count());
        config.put("totalJobs", stores.jobStore.count());
        config.put("totalMatches", stores.matchRecordStore.count());
        config.put("recentLogCount", stores.operationLogStore.count());
        return Result.success(config);
    }

    @Operation(summary = "获取所有可编辑配置项")
    @GetMapping("/items")
    public Result<List<SystemConfigEntity>> getConfigItems() {
        return Result.success(stores.systemConfigStore.findAll());
    }

    @Operation(summary = "更新配置项")
    @PutMapping("/items/{id}")
    public Result<?> updateConfigItem(@PathVariable Long id, @RequestBody SystemConfigEntity entity) {
        SystemConfigEntity existing = stores.systemConfigStore.findById(id);
        if (existing == null) return Result.error("配置项不存在");
        existing.setConfigValue(entity.getConfigValue());
        stores.systemConfigStore.update(existing);
        return Result.success("配置已更新");
    }

    @Operation(summary = "获取最近操作日志")
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        List<OperationLog> all = stores.operationLogStore.findAll();
        all.sort(Comparator.comparing(OperationLog::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<OperationLog> pageList = from < total ? all.subList(from, to) : List.of();
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        return Result.success(result);
    }
}
