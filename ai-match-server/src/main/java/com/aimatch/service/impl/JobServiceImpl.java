package com.aimatch.service.impl;

import cn.hutool.json.JSONUtil;
import com.aimatch.ai.AiParseService;
import com.aimatch.model.entity.Job;
import com.aimatch.model.entity.User;
import com.aimatch.service.CapabilityGraphService;
import com.aimatch.service.FileService;
import com.aimatch.service.JobService;
import com.aimatch.store.DataStores;
import com.aimatch.util.ParsedDataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final DataStores stores;
    private final FileService fileService;
    private final AiParseService aiParseService;
    private final CapabilityGraphService capabilityGraphService;

    @Override
    public Map<String, Object> uploadAndParse(MultipartFile file, Long hrId) {
        String text = fileService.extractText(file);
        Map<String, Object> parsedData = aiParseService.parseJobDescription(text);

        Job job = buildJobFromParsedData(parsedData, hrId);
        job.setRawText(text);
        job.setParsedJson(JSONUtil.toJsonStr(parsedData));
        job.setStatus(1);
        stores.jobStore.save(job);

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getId());
        result.put("parsedData", parsedData);
        return result;
    }

    @Override
    public Map<String, Object> searchJobs(String keyword, String industry, String city, String education, Integer workYears, int page, int size) {
        List<Job> filtered = stores.jobStore.findAll(j -> {
            if (j.getStatus() == null || j.getStatus() != 1) return false;
            if (keyword != null && !keyword.isEmpty()) {
                boolean match = (j.getTitle() != null && j.getTitle().contains(keyword))
                        || (j.getCompany() != null && j.getCompany().contains(keyword))
                        || (j.getDescription() != null && j.getDescription().contains(keyword));
                if (!match) return false;
            }
            if (industry != null && !industry.isEmpty() && j.getIndustry() != null && !j.getIndustry().contains(industry)) return false;
            if (city != null && !city.isEmpty() && j.getCity() != null && !j.getCity().contains(city)) return false;
            if (education != null && !education.isEmpty() && j.getEducation() != null && !j.getEducation().contains(education)) return false;
            if (workYears != null && j.getWorkYears() != null && j.getWorkYears() < workYears) return false;
            return true;
        });

        filtered.sort(Comparator.comparing(Job::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Job> pageList = from < total ? filtered.subList(from, to) : List.of();

        List<Map<String, Object>> records = pageList.stream().map(j -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("title", j.getTitle());
            m.put("company", j.getCompany());
            m.put("city", j.getCity());
            m.put("salary", j.getSalary());
            m.put("education", j.getEducation());
            m.put("workYears", j.getWorkYears());
            m.put("industry", j.getIndustry());
            m.put("jobType", j.getJobType());
            m.put("skillRequirements", j.getSkillRequirements());
            m.put("createTime", j.getCreateTime());
            m.put("headCount", j.getHeadCount());
            m.put("department", j.getDepartment());
            m.put("description", j.getDescription());
            m.put("requirement", j.getRequirement());
            m.put("count", stores.matchRecordStore.count(r -> r.getJobId() != null && r.getJobId().equals(j.getId())));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", page);
        result.put("pages", (total + size - 1) / size);
        return result;
    }

    @Override
    public Map<String, Object> getJobDetail(Long jobId) {
        Job job = stores.jobStore.findById(jobId);
        if (job == null) throw new RuntimeException("职位不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("id", job.getId());
        result.put("hrId", job.getHrId());
        result.put("title", job.getTitle());
        result.put("company", job.getCompany());
        result.put("department", job.getDepartment());
        result.put("jobType", job.getJobType());
        result.put("industry", job.getIndustry());
        result.put("salary", job.getSalary());
        result.put("city", job.getCity());
        result.put("education", job.getEducation());
        result.put("workYears", job.getWorkYears());
        result.put("description", job.getDescription());
        result.put("requirement", job.getRequirement());
        result.put("skillRequirements", job.getSkillRequirements());
        result.put("headCount", job.getHeadCount());
        result.put("createTime", job.getCreateTime());
        result.put("parsedData", job.getParsedJson() != null ? JSONUtil.parseObj(job.getParsedJson()) : null);
        return result;
    }

    @Override
    public Map<String, Object> getJobCapability(Long jobId) {
        return capabilityGraphService.buildJobCapabilityGraph(jobId);
    }

    @Override
    public void deleteJob(Long jobId) {
        Job job = stores.jobStore.findById(jobId);
        if (job == null) throw new RuntimeException("职位不存在");
        job.setStatus(0);
        stores.jobStore.update(job);
    }

    @Override
    public void updateJob(Long jobId, Map<String, Object> updateData) {
        Job job = stores.jobStore.findById(jobId);
        if (job == null) throw new RuntimeException("职位不存在");
        if (updateData.containsKey("title")) job.setTitle((String) updateData.get("title"));
        if (updateData.containsKey("description")) job.setDescription((String) updateData.get("description"));
        if (updateData.containsKey("requirement")) job.setRequirement((String) updateData.get("requirement"));
        if (updateData.containsKey("skillRequirements")) job.setSkillRequirements((String) updateData.get("skillRequirements"));
        if (updateData.containsKey("salary")) job.setSalary((String) updateData.get("salary"));
        if (updateData.containsKey("city")) job.setCity((String) updateData.get("city"));
        if (updateData.containsKey("company")) job.setCompany((String) updateData.get("company"));
        if (updateData.containsKey("department")) job.setDepartment((String) updateData.get("department"));
        if (updateData.containsKey("education")) job.setEducation((String) updateData.get("education"));
        if (updateData.containsKey("workYears") && updateData.get("workYears") != null) job.setWorkYears(Integer.valueOf(updateData.get("workYears").toString()));
        if (updateData.containsKey("headCount") && updateData.get("headCount") != null) job.setHeadCount(Integer.valueOf(updateData.get("headCount").toString()));
        if (updateData.containsKey("jobType")) job.setJobType((String) updateData.get("jobType"));
        if (updateData.containsKey("industry")) job.setIndustry((String) updateData.get("industry"));
        stores.jobStore.update(job);
    }

    private Job buildJobFromParsedData(Map<String, Object> data, Long hrId) {
        Job job = new Job();
        job.setHrId(hrId);
        // Set company: prefer HR profile company, fallback to AI parsed data
        String hrCompany = null;
        try {
            com.aimatch.model.entity.User hr = stores.userStore.findById(hrId);
            if (hr != null && hr.getCompany() != null && !hr.getCompany().isEmpty()) {
                hrCompany = hr.getCompany();
            }
        } catch (Exception ignored) {}
        job.setCompany(hrCompany != null ? hrCompany : ParsedDataHelper.getString(data, "company"));
        job.setTitle(ParsedDataHelper.getString(data, "title"));
        job.setDepartment(ParsedDataHelper.getString(data, "department"));
        job.setJobType(ParsedDataHelper.getString(data, "jobType"));
        job.setIndustry(ParsedDataHelper.getString(data, "industry"));
        job.setCity(ParsedDataHelper.getString(data, "city"));
        job.setEducation(ParsedDataHelper.getString(data, "education"));
        job.setWorkYears(ParsedDataHelper.getInt(data, "workYears"));
        job.setSalary(ParsedDataHelper.getString(data, "salary"));
        job.setDescription(ParsedDataHelper.getString(data, "description"));
        job.setRequirement(ParsedDataHelper.getString(data, "requirement"));
        job.setSkillRequirements(data.get("skillRequirements") != null ? JSONUtil.toJsonStr(data.get("skillRequirements")) : null);
        Integer hc = ParsedDataHelper.getInt(data, "headCount");
        job.setHeadCount(hc != null && hc > 0 ? hc : null);
        return job;
    }
}


