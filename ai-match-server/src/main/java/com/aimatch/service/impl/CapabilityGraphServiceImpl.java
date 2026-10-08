package com.aimatch.service.impl;

import com.aimatch.util.ParsedDataHelper;
import com.aimatch.model.entity.Job;
import com.aimatch.model.entity.Resume;
import com.aimatch.model.entity.Skill;
import com.aimatch.service.CapabilityGraphService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilityGraphServiceImpl implements CapabilityGraphService {

    private final DataStores stores;

    @Override
    public Map<String, Object> buildPersonCapabilityGraph(Long userId, Long resumeId) {
        Resume resume = stores.resumeStore.findById(resumeId);
        if (resume == null) {
            throw new RuntimeException("resume not found");
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        String personId = "person_" + resume.getId();
        nodes.add(Map.of("id", personId, "label", resume.getName() != null ? resume.getName() : "Jobseeker",
                "group", "person", "symbolSize", 40));

        List<Map<String, Object>> resumeSkills = ParsedDataHelper.parseSkillList(resume.getSkills());
        Map<String, Skill> skillMap = loadSkillMap();
        Set<String> addedSkillIds = new HashSet<>();

        for (Map<String, Object> rs : resumeSkills) {
            String skillName = (String) rs.get("name");
            if (skillName == null || skillName.isBlank()) continue;

            Skill matched = findSkillByName(skillMap, skillName);
            String skillNodeId;
            if (matched != null) {
                skillNodeId = "skill_" + matched.getId();
                if (addedSkillIds.add(skillNodeId)) {
                    int level = rs.containsKey("level") ? ((Number) rs.get("level")).intValue() : 3;
                    nodes.add(Map.of("id", skillNodeId, "label", matched.getName(),
                            "group", "skill", "symbolSize", 10 + level * 6,
                            "category", matched.getCategory() != null ? matched.getCategory() : "",
                            "level", level));
                    addParentSkillEdges(nodes, edges, matched, skillMap, addedSkillIds);
                }
                edges.add(Map.of("source", personId, "target", skillNodeId, "label", "HAS_SKILL"));
            } else {
                skillNodeId = "skill_custom_" + skillName;
                if (addedSkillIds.add(skillNodeId)) {
                    nodes.add(Map.of("id", skillNodeId, "label", skillName,
                            "group", "skill", "symbolSize", 20));
                }
                edges.add(Map.of("source", personId, "target", skillNodeId, "label", "HAS_SKILL"));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("person", Map.of("userId", userId, "name", resume.getName()));
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @Override
    public Map<String, Object> buildJobCapabilityGraph(Long jobId) {
        Job job = stores.jobStore.findById(jobId);
        if (job == null) {
            throw new RuntimeException("job not found");
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        String jobNodeId = "job_" + job.getId();
        nodes.add(Map.of("id", jobNodeId, "label", job.getTitle() != null ? job.getTitle() : "Job",
                "group", "job", "symbolSize", 40));

        List<Map<String, Object>> jobSkills = ParsedDataHelper.parseSkillList(job.getSkillRequirements());
        Map<String, Skill> skillMap = loadSkillMap();
        Set<String> addedSkillIds = new HashSet<>();

        for (Map<String, Object> js : jobSkills) {
            String skillName = (String) js.get("name");
            if (skillName == null || skillName.isBlank()) continue;

            Skill matched = findSkillByName(skillMap, skillName);
            String skillNodeId;
            if (matched != null) {
                skillNodeId = "skill_" + matched.getId();
                if (addedSkillIds.add(skillNodeId)) {
                    int importance = js.containsKey("importance") ? ((Number) js.get("importance")).intValue() : 3;
                    nodes.add(Map.of("id", skillNodeId, "label", matched.getName(),
                            "group", "skill", "symbolSize", 10 + importance * 6,
                            "category", matched.getCategory() != null ? matched.getCategory() : "",
                            "importance", importance));
                    addParentSkillEdges(nodes, edges, matched, skillMap, addedSkillIds);
                }
                edges.add(Map.of("source", jobNodeId, "target", skillNodeId, "label", "要求"));
            } else {
                skillNodeId = "skill_custom_" + skillName;
                if (addedSkillIds.add(skillNodeId)) {
                    nodes.add(Map.of("id", skillNodeId, "label", skillName,
                            "group", "skill", "symbolSize", 20));
                }
                edges.add(Map.of("source", jobNodeId, "target", skillNodeId, "label", "要求"));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("job", Map.of("jobId", jobId, "title", job.getTitle()));
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }


    @Override
    public Map<String, Object> getCapabilityGraphData() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> addedSkillIds = new HashSet<>();

        List<Resume> resumes = stores.resumeStore.findAll(r -> r.getStatus() != null && r.getStatus() == 1);
        List<Job> jobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);
        Map<String, Skill> skillMap = loadSkillMap();

        for (Resume r : resumes) {
            String personId = "person_" + r.getId();
            nodes.add(Map.of("id", personId, "label", r.getName() != null ? r.getName() : "Candidate",
                    "group", "person", "symbolSize", 30));

            List<Map<String, Object>> resumeSkills = ParsedDataHelper.parseSkillList(r.getSkills());
            for (Map<String, Object> rs : resumeSkills) {
                String skillName = (String) rs.get("name");
                if (skillName == null || skillName.isBlank()) continue;
                Skill matched = findSkillByName(skillMap, skillName);
                String skillNodeId;
                if (matched != null) {
                    skillNodeId = "skill_" + matched.getId();
                    if (addedSkillIds.add(skillNodeId)) {
                        nodes.add(Map.of("id", skillNodeId, "label", matched.getName(),
                                "group", "skill", "symbolSize", 20));
                    }
                } else {
                    skillNodeId = "skill_custom_" + skillName;
                    if (addedSkillIds.add(skillNodeId)) {
                        nodes.add(Map.of("id", skillNodeId, "label", skillName,
                                "group", "skill", "symbolSize", 15));
                    }
                }
                edges.add(Map.of("source", personId, "target", skillNodeId, "label", "HAS"));
            }
        }

        for (Job j : jobs) {
            String jobNodeId = "job_" + j.getId();
            nodes.add(Map.of("id", jobNodeId, "label", j.getTitle() != null ? j.getTitle() : "Position",
                    "group", "job", "symbolSize", 30));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @Override
    public Map<String, Object> getMyGraphData(Long userId) {
        List<Resume> resumes = stores.resumeStore.findAll(r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
        if (resumes.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("nodes", List.of());
            empty.put("edges", List.of());
            return empty;
        }
        return buildPersonCapabilityGraph(userId, resumes.get(0).getId());
    }

    @Override
    public Map<String, Object> getGapGraph(Long resumeId, Long jobId) {
        return buildMatchComparisonGraph(resumeId, jobId);
    }

    private Map<String, Object> buildMatchComparisonGraph(Long personId, Long jobId) {
        Resume resume = stores.resumeStore.findById(personId);
        Job job = stores.jobStore.findById(jobId);
        if (resume == null || job == null) {
            throw new RuntimeException("resume or job not found");
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        String personNodeId = "person_" + personId;
        String jobNodeId = "job_" + jobId;
        nodes.add(Map.of("id", personNodeId, "label", resume.getName() != null ? resume.getName() : "Candidate",
                "group", "person", "symbolSize", 40));
        nodes.add(Map.of("id", jobNodeId, "label", job.getTitle() != null ? job.getTitle() : "Position",
                "group", "job", "symbolSize", 40));

        List<Map<String, Object>> resumeSkills = ParsedDataHelper.parseSkillList(resume.getSkills());
        List<Map<String, Object>> jobSkills = ParsedDataHelper.parseSkillList(job.getSkillRequirements());
        Map<String, Skill> skillMap = loadSkillMap();
        Set<String> addedSkillIds = new HashSet<>();
        Set<String> addedNodeIds = new HashSet<>();

        for (Map<String, Object> js : jobSkills) {
            String skillName = (String) js.get("name");
            if (skillName == null || skillName.isBlank()) continue;
            Skill matched = findSkillByName(skillMap, skillName);
            boolean isMissing = resumeSkills.stream().noneMatch(rs ->
                    skillName.equals(rs.get("name")) || (matched != null && rs.get("name") != null &&
                            matched.getName().equalsIgnoreCase((String) rs.get("name"))));
            addSkillNodeWithStatus(nodes, edges, jobNodeId, matched, skillName, "要求",
                    isMissing, skillMap, addedSkillIds, addedNodeIds);
        }

        for (Map<String, Object> rs : resumeSkills) {
            String skillName = (String) rs.get("name");
            if (skillName == null || skillName.isBlank()) continue;
            Skill matched = findSkillByName(skillMap, skillName);
            addSkillNodeWithStatus(nodes, edges, personNodeId, matched, skillName, "HAS_SKILL",
                    false, skillMap, addedSkillIds, addedNodeIds);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("person", Map.of("id", personId, "name", resume.getName()));
        result.put("job", Map.of("id", jobId, "title", job.getTitle()));
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private void addSkillNodeWithStatus(List<Map<String, Object>> nodes, List<Map<String, Object>> edges,
                                         String sourceId, Skill matched, String skillName, String edgeLabel,
                                         boolean isMissing, Map<String, Skill> skillMap,
                                         Set<String> addedSkillIds, Set<String> addedNodeIds) {
        String skillNodeId;
        if (matched != null) {
            skillNodeId = "skill_" + matched.getId();
            if (addedNodeIds.add(skillNodeId)) {
                Map<String, Object> nodeAttrs = new LinkedHashMap<>();
                nodeAttrs.put("id", skillNodeId);
                nodeAttrs.put("label", matched.getName());
                nodeAttrs.put("group", isMissing ? "missing_skill" : "skill");
                nodeAttrs.put("symbolSize", isMissing ? 30 : 22);
                if (isMissing) {
                    nodeAttrs.put("itemStyle", Map.of("color", "#f56c6c"));
                }
                nodes.add(nodeAttrs);
                if (!isMissing) {
                    addParentSkillEdges(nodes, edges, matched, skillMap, addedSkillIds);
                }
            }
        } else {
            skillNodeId = "skill_custom_" + skillName;
            if (addedNodeIds.add(skillNodeId)) {
                Map<String, Object> nodeAttrs = new LinkedHashMap<>();
                nodeAttrs.put("id", skillNodeId);
                nodeAttrs.put("label", skillName);
                nodeAttrs.put("group", isMissing ? "missing_skill" : "skill");
                nodeAttrs.put("symbolSize", isMissing ? 30 : 22);
                if (isMissing) {
                    nodeAttrs.put("itemStyle", Map.of("color", "#f56c6c"));
                }
                nodes.add(nodeAttrs);
            }
        }

        edges.add(Map.of("source", sourceId, "target", skillNodeId,
                "label", isMissing ? "MISSING" : edgeLabel,
                "lineStyle", isMissing ? Map.of("color", "#f56c6c", "type", "dashed") : Map.of()));
    }

    private void addParentSkillEdges(List<Map<String, Object>> nodes, List<Map<String, Object>> edges,
                                      Skill skill, Map<String, Skill> skillMap, Set<String> addedIds) {
        if (skill.getParentId() != null && skill.getParentId() > 0) {
            Skill parent = skillMap.get(skill.getParentId());
            if (parent != null) {
                String parentNodeId = "skill_" + parent.getId();
                if (addedIds.add(parentNodeId)) {
                    nodes.add(Map.of("id", parentNodeId, "label", parent.getName(),
                            "group", "skill_category", "symbolSize", 35,
                            "category", parent.getCategory() != null ? parent.getCategory() : ""));
                    addParentSkillEdges(nodes, edges, parent, skillMap, addedIds);
                }
                edges.add(Map.of("source", "skill_" + skill.getId(), "target", parentNodeId, "label", "BELONGS_TO"));
            }
        }
    }

    private Map<String, Skill> loadSkillMap() {
        Map<String, Skill> map = new LinkedHashMap<>();
        try {
            List<Skill> skills = stores.skillStore.findAll();
            for (Skill s : skills) {
                map.put(s.getId().toString(), s);
            }
        } catch (Exception e) {
            log.warn("load skill map failed: {}", e.getMessage());
        }
        return map;
    }

    private Skill findSkillByName(Map<String, Skill> skillMap, String name) {
        if (name == null) return null;
        String lower = name.trim().toLowerCase();
        for (Skill s : skillMap.values()) {
            if (s.getName() != null && s.getName().toLowerCase().equals(lower)) {
                return s;
            }
        }
        for (Skill s : skillMap.values()) {
            if (s.getName() != null && s.getName().toLowerCase().contains(lower)) {
                return s;
            }
        }
        return null;
    }
}

