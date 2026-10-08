package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.*;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "人才收藏", description = "HR人才收藏、分类、备注管理")
@RestController
@RequestMapping("/api/hr/favorites")
@RequiredArgsConstructor
public class TalentFavoriteController {

    private final DataStores stores;

    @Operation(summary = "获取我的收藏列表")
    @GetMapping
    public Result<List<Map<String, Object>>> getFavorites(
            Principal principal,
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category) {
        Long hrId = Long.parseLong(principal.getName());

        List<TalentFavorite> favorites = stores.talentFavoriteStore.findAll(f -> f.getHrId().equals(hrId));
        if (category != null && !category.isEmpty()) {
            favorites = favorites.stream().filter(f -> category.equals(f.getCategory())).collect(Collectors.toList());
        }
        favorites.sort(Comparator.comparing(TalentFavorite::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> result = favorites.stream().map(fav -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fav.getId());
            map.put("resumeId", fav.getResumeId());
            map.put("category", fav.getCategory());
            map.put("notes", fav.getNotes());
            map.put("createTime", fav.getCreateTime());

            Resume resume = stores.resumeStore.findById(fav.getResumeId());
            if (resume != null) {
                map.put("name", resume.getName());
                map.put("education", resume.getEducation());
                map.put("workYears", resume.getWorkYears());
                map.put("currentPosition", resume.getCurrentPosition());
                map.put("skills", resume.getSkills());
                map.put("expectedCity", resume.getExpectedCity());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Operation(summary = "收藏候选人")
    @PostMapping("/{resumeId}")
    public Result<?> addFavorite(@PathVariable Long resumeId, Principal principal) {
        Long hrId = Long.parseLong(principal.getName());

        boolean exists = stores.talentFavoriteStore.findOne(f -> f.getHrId().equals(hrId) && f.getResumeId().equals(resumeId)) != null;
        if (exists) {
            return Result.error("已收藏该候选人");
        }

        TalentFavorite fav = new TalentFavorite();
        fav.setHrId(hrId);
        fav.setResumeId(resumeId);
        fav.setCategory("意向");
        stores.talentFavoriteStore.save(fav);
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{resumeId}")
    public Result<?> removeFavorite(@PathVariable Long resumeId, Principal principal) {
        Long hrId = Long.parseLong(principal.getName());
        var list = stores.talentFavoriteStore.findAll(f -> f.getHrId().equals(hrId) && f.getResumeId().equals(resumeId));
        for (var f : list) {
            stores.talentFavoriteStore.deleteById(f.getId());
        }
        return Result.success("已取消收藏");
    }

    @Operation(summary = "更新候选人分类")
    @PutMapping("/{id}/category")
    public Result<?> updateCategory(@PathVariable Long id,
                                     @Parameter(description = "分类: 意向/面试/录用") @RequestParam String category) {
        TalentFavorite fav = stores.talentFavoriteStore.findById(id);
        if (fav == null) return Result.error("记录不存在");
        fav.setCategory(category);
        stores.talentFavoriteStore.update(fav);
        return Result.success("分类已更新");
    }

    @Operation(summary = "更新备注")
    @PutMapping("/{id}/notes")
    public Result<?> updateNotes(@PathVariable Long id,
                                  @Parameter(description = "备注内容") @RequestBody Map<String, String> body) {
        TalentFavorite fav = stores.talentFavoriteStore.findById(id);
        if (fav == null) return Result.error("记录不存在");
        fav.setNotes(body.get("notes"));
        stores.talentFavoriteStore.update(fav);
        return Result.success("备注已更新");
    }
}
