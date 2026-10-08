package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@Tag(name = "简历管理", description = "简历上传、AI解析、编辑管理")
@RestController
@RequestMapping("/api/jobseeker/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传并AI解析简历")
    @PostMapping("/upload")
    public Result<?> uploadResume(@Parameter(description = "简历文件") @RequestParam("file") MultipartFile file, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(resumeService.uploadAndParse(file, userId));
    }

    @Operation(summary = "批量上传简历")
    @PostMapping("/batch-upload")
    public Result<?> batchUploadResume(@Parameter(description = "简历文件") @RequestParam("files") List<MultipartFile> files, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Map<String, Object> result = resumeService.uploadAndParse(file, userId);
                result.put("fileName", file.getOriginalFilename());
                result.put("status", "success");
                results.add(result);
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("fileName", file.getOriginalFilename());
                err.put("status", "failed");
                err.put("error", e.getMessage());
                results.add(err);
            }
        }
        return Result.success(results);
    }

    @Operation(summary = "获取简历详情")
    @GetMapping("/{resumeId}")
    public Result<?> getResume(@Parameter(description = "简历ID") @PathVariable Long resumeId) {
        return Result.success(resumeService.getResumeDetail(resumeId));
    }

    @Operation(summary = "更新简历信息")
    @PutMapping("/{resumeId}")
    public Result<?> updateResume(@Parameter(description = "简历ID") @PathVariable Long resumeId, @RequestBody Map<String, Object> data) {
        resumeService.updateResume(resumeId, data);
        return Result.success("更新成功");
    }

    @Operation(summary = "获取个人能力画像")
    @GetMapping("/my-capability")
    public Result<?> getMyCapability(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(resumeService.getPersonalCapability(userId));
    }
}