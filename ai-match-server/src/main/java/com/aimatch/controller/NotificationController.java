package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.Notification;
import com.aimatch.store.DataStores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "系统通知", description = "用户通知查询与管理")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final DataStores stores;

    @Operation(summary = "获取我的通知列表")
    @GetMapping
    public Result<Map<String, Object>> getNotifications(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        List<Notification> list = stores.notificationStore.findAll(n -> n.getUserId().equals(userId));
        list.sort(Comparator.comparing(Notification::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        List<Notification> pageList = list.stream().limit(50).collect(Collectors.toList());

        long unreadCount = list.stream().filter(n -> n.getIsRead() == 0).count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("unreadCount", unreadCount);
        return Result.success(result);
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        long count = stores.notificationStore.count(n -> n.getUserId().equals(userId) && n.getIsRead() == 0);
        return Result.success(count);
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        Notification n = stores.notificationStore.findById(id);
        if (n != null) {
            n.setIsRead(1);
            stores.notificationStore.update(n);
        }
        return Result.success("已标记已读");
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<?> markAllRead(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        var list = stores.notificationStore.findAll(n -> n.getUserId().equals(userId) && n.getIsRead() == 0);
        for (Notification n : list) {
            n.setIsRead(1);
            stores.notificationStore.update(n);
        }
        return Result.success("全部已读");
    }
}
