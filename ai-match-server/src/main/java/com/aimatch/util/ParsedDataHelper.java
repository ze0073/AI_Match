package com.aimatch.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import java.util.*;

public final class ParsedDataHelper {

    private ParsedDataHelper() {}

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    public static Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    public static Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    public static List<Map<String, Object>> parseSkillList(String skillsStr) {
        List<Map<String, Object>> skills = new ArrayList<>();
        if (skillsStr == null || skillsStr.isEmpty()) return skills;

        try {
            JSONArray arr = JSONUtil.parseArray(skillsStr);
            for (int i = 0; i < arr.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                if (arr.get(i) instanceof String) {
                    item.put("name", arr.getStr(i));
                    item.put("level", 3);
                } else {
                    item.putAll(arr.getJSONObject(i));
                }
                skills.add(item);
            }
        } catch (Exception e) {
            for (String s : skillsStr.split("[,，;；]")) {
                if (s.trim().isEmpty()) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", s.trim());
                item.put("level", 3);
                skills.add(item);
            }
        }
        return skills;
    }

    public static String getFileExtension(String filename) {
        if (filename == null) return "unknown";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "unknown";
    }
}
