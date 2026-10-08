package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.OperationLog;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "操作日志", description = "系统操作日志查询")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final DataStores stores;

    @Operation(summary = "查询操作日志")
    @GetMapping
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType) {
        List<OperationLog> filtered = stores.operationLogStore.findAll(log -> {
            if (username != null && !username.isEmpty() && log.getUsername() != null && !log.getUsername().contains(username))
                return false;
            if (operationType != null && !operationType.isEmpty() && !operationType.equals(log.getOperationType()))
                return false;
            return true;
        });
        filtered.sort(Comparator.comparing(OperationLog::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        int total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<OperationLog> pageList = from < total ? filtered.subList(from, to) : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        return Result.success(result);
    }

    @Operation(summary = "导出操作日志CSV")
    @GetMapping("/export")
    public void exportLogs(HttpServletResponse response) throws Exception {
        var logs = stores.operationLogStore.findAll();
        logs.sort(Comparator.comparing(OperationLog::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=operation_logs.csv");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("\uFEFFID,用户名,操作类型,模块,描述,IP地址,耗时(ms),结果,时间");
        for (OperationLog log : logs) {
            pw.printf("%d,%s,%s,%s,%s,%s,%d,%s,%s%n",
                    log.getId(), log.getUsername(), log.getOperationType(), log.getModule(),
                    escapeCsv(log.getDescription()), log.getIpAddress(),
                    log.getCostTime(), log.getResult(), log.getCreateTime());
        }
        pw.flush();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
