package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.entity.User;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataStores stores;

    @GetMapping("/users")
    public Result<?> getUsers(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String userType) {
        List<User> filtered = stores.userStore.findAll(user -> {
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (user.getUsername() != null && user.getUsername().contains(keyword))
                        || (user.getRealName() != null && user.getRealName().contains(keyword))
                        || (user.getPhone() != null && user.getPhone().contains(keyword));
                if (!match) return false;
            }
            if (userType != null && !userType.isEmpty() && !userType.equals(user.getUserType())) return false;
            return true;
        });

        filtered.sort(Comparator.comparing(User::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        int total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<User> pageList = from < total ? filtered.subList(from, to) : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return Result.success(result);
    }

    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        var user = stores.userStore.findById(userId);
        if (user == null) {
            return Result.error("?????");
        }
        user.setStatus(status);
        stores.userStore.update(user);
        return Result.success("??????");
    }

    @PutMapping("/users/{userId}")
    public Result<?> updateUser(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        User user = stores.userStore.findById(userId);
        if (user == null) {
            return Result.error("?????");
        }
        if (body.containsKey("realName")) user.setRealName((String) body.get("realName"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("company")) user.setCompany((String) body.get("company"));
        stores.userStore.update(user);
        return Result.success("???????");
    }
}
