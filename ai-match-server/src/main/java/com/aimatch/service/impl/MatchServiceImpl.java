package com.aimatch.service.impl;

import cn.hutool.json.JSONUtil;
import com.aimatch.ai.AiParseService;
import com.aimatch.config.MatchCacheManager;
import com.aimatch.model.entity.Job;
import com.aimatch.model.entity.MatchRecord;
import com.aimatch.model.entity.Resume;
import com.aimatch.service.MatchService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final DataStores stores;
    private final AiParseService aiParseService;
    private final MatchCacheManager matchCacheManager;

    @Override
    public Map<String, Object> matchPersonToJobs(Long resumeId, Long userId) {
        Resume resume = stores.resumeStore.findById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }
        if (!resume.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此简历");
        }

        List<Job> jobs = stores.jobStore.findAll(j -> j.getStatus() != null && j.getStatus() == 1);

        if (jobs.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("resumeId", resumeId);
            result.put("total", 0);
            result.put("results", Collections.emptyList());
            return result;
        }

        List<Map<String, Object>> matchResults = new ArrayList<>();

        for (Job job : jobs) {
            Map<String, Object> personSkills = parseSkills(resume.getSkills(), resume.getParsedJson());
            Map<String, Object> jobRequirements = parseSkills(job.getSkillRequirements(), job.getParsedJson());

            String cacheKey = matchCacheManager.buildMatchKey(resumeId, job.getId());
            Double cachedScore = matchCacheManager.get(cacheKey);
            double score = cachedScore != null ? cachedScore
                    : aiParseService.calculateMatchScore(personSkills, jobRequirements);
            if (cachedScore == null) {
                matchCacheManager.put(cacheKey, score);
            }

            Map<String, Object> gapAnalysis = aiParseService.analyzeSkillGap(personSkills, jobRequirements);

            MatchRecord record = new MatchRecord();
            record.setResumeId(resumeId);
            record.setJobId(job.getId());
            record.setUserId(userId);
            record.setMatchScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
            record.setMatchDetail(JSONUtil.toJsonStr(gapAnalysis));
            record.setMatchType("PERSON_TO_JOB");
            record.setStatus(1);
            stores.matchRecordStore.save(record);

            Map<String, Object> matchItem = new HashMap<>();
            matchItem.put("matchId", record.getId());
            matchItem.put("jobId", job.getId());
            matchItem.put("jobTitle", job.getTitle());
            matchItem.put("company", job.getCompany());
            matchItem.put("salary", job.getSalary());
            matchItem.put("city", job.getCity());
            matchItem.put("matchScore", score);
            matchItem.put("matchDetail", gapAnalysis);
            matchResults.add(matchItem);
        }

        matchResults.sort((a, b) -> Double.compare(
                (Double) b.get("matchScore"), (Double) a.get("matchScore")));

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resumeId);
        result.put("total", matchResults.size());
        result.put("results", matchResults);
        return result;
    }

    @Override
    public Map<String, Object> matchJobToCandidates(Long jobId) {
        Job job = stores.jobStore.findById(jobId);
        if (job == null) {
            throw new RuntimeException("职位不存在");
        }

        List<Resume> resumes = stores.resumeStore.findAll(r -> r.getStatus() != null && r.getStatus() == 1);

        if (resumes.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("jobId", jobId);
            result.put("jobTitle", job.getTitle());
            result.put("total", 0);
            result.put("results", Collections.emptyList());
            return result;
        }

        List<Map<String, Object>> matchResults = new ArrayList<>();

        for (Resume resume : resumes) {
            Map<String, Object> personSkills = parseSkills(resume.getSkills(), resume.getParsedJson());
            Map<String, Object> jobRequirements = parseSkills(job.getSkillRequirements(), job.getParsedJson());

            String cacheKey = matchCacheManager.buildMatchKey(resume.getId(), jobId);
            Double cachedScore = matchCacheManager.get(cacheKey);
            double score = cachedScore != null ? cachedScore
                    : aiParseService.calculateMatchScore(personSkills, jobRequirements);
            if (cachedScore == null) {
                matchCacheManager.put(cacheKey, score);
            }

            Map<String, Object> gapAnalysis = aiParseService.analyzeSkillGap(personSkills, jobRequirements);

            MatchRecord record = new MatchRecord();
            record.setResumeId(resume.getId());
            record.setJobId(jobId);
            record.setUserId(resume.getUserId());
            record.setMatchScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
            record.setMatchDetail(JSONUtil.toJsonStr(gapAnalysis));
            record.setMatchType("JOB_TO_PERSON");
            record.setStatus(1);
            stores.matchRecordStore.save(record);

            Map<String, Object> matchItem = new HashMap<>();
            matchItem.put("matchId", record.getId());
            matchItem.put("resumeId", resume.getId());
            matchItem.put("userName", resume.getName());
            matchItem.put("education", resume.getEducation());
            matchItem.put("workYears", resume.getWorkYears());
            matchItem.put("currentPosition", resume.getCurrentPosition());
            matchItem.put("matchScore", score);
            matchItem.put("matchDetail", gapAnalysis);
            matchResults.add(matchItem);
        }

        matchResults.sort((a, b) -> Double.compare(
                (Double) b.get("matchScore"), (Double) a.get("matchScore")));

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("jobTitle", job.getTitle());
        result.put("total", matchResults.size());
        result.put("results", matchResults);
        return result;
    }

    @Override
    public List<Map<String, Object>> getRecommendationsForJobseeker(Long userId, int limit) {
        List<MatchRecord> records = stores.matchRecordStore.page(1, limit,
                r -> r.getUserId().equals(userId) && "PERSON_TO_JOB".equals(r.getMatchType()),
                Comparator.comparing(MatchRecord::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> results = new ArrayList<>();
        for (MatchRecord record : records) {
            Job job = stores.jobStore.findById(record.getJobId());
            if (job == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("matchId", record.getId());
            item.put("jobId", job.getId());
            item.put("jobTitle", job.getTitle());
            item.put("company", job.getCompany());
            item.put("salary", job.getSalary());
            item.put("city", job.getCity());
            item.put("industry", job.getIndustry());
            item.put("matchScore", record.getMatchScore());
            item.put("matchDetail", record.getMatchDetail() != null ?
                    JSONUtil.parseObj(record.getMatchDetail()) : null);
            results.add(item);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getRecommendationsForHr(Long jobId, int limit) {
        List<MatchRecord> records = stores.matchRecordStore.page(1, limit,
                r -> r.getJobId().equals(jobId) && ("PERSON_TO_JOB".equals(r.getMatchType()) || "JOB_TO_PERSON".equals(r.getMatchType())),
                Comparator.comparing(MatchRecord::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> results = new ArrayList<>();
        for (MatchRecord record : records) {
            Resume resume = stores.resumeStore.findById(record.getResumeId());
            if (resume == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("matchId", record.getId());
            item.put("resumeId", resume.getId());
            item.put("userName", resume.getName());
            item.put("education", resume.getEducation());
            item.put("workYears", resume.getWorkYears());
            item.put("currentPosition", resume.getCurrentPosition());
            item.put("matchScore", record.getMatchScore());
            item.put("matchDetail", record.getMatchDetail() != null ?
                    JSONUtil.parseObj(record.getMatchDetail()) : null);
            results.add(item);
        }
        return results;
    }

    @Override
    public Map<String, Object> getMatchDetail(Long matchId) {
        MatchRecord record = stores.matchRecordStore.findById(matchId);
        if (record == null) {
            throw new RuntimeException("匹配记录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("matchId", record.getId());
        result.put("matchScore", record.getMatchScore());
        result.put("matchDetail", record.getMatchDetail() != null ?
                JSONUtil.parseObj(record.getMatchDetail()) : null);

        Resume resume = stores.resumeStore.findById(record.getResumeId());
        Job job = stores.jobStore.findById(record.getJobId());

        if (resume != null) {
            result.put("resumeId", resume.getId());
            result.put("candidateName", resume.getName());
            result.put("candidateSkills", resume.getSkills());
            result.put("education", resume.getEducation());
            result.put("workYears", resume.getWorkYears());
        }
        if (job != null) {
            result.put("jobId", job.getId());
            result.put("jobTitle", job.getTitle());
            result.put("company", job.getCompany());
            result.put("jobSkills", job.getSkillRequirements());
            result.put("salary", job.getSalary());
            result.put("city", job.getCity());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSkills(String skillsStr, String parsedJson) {
        Map<String, Object> result = new HashMap<>();
        result.put("skills", skillsStr != null ? skillsStr : "");

        if (parsedJson != null) {
            try {
                Map<String, Object> parsed = JSONUtil.parseObj(parsedJson);
                result.putAll(parsed);
            } catch (Exception e) {
                log.warn("解析parsedJson失败", e);
            }
        }
        return result;
    }
}
