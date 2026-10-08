package com.aimatch.service.impl;

import cn.hutool.json.JSONUtil;
import com.aimatch.ai.AiParseService;
import com.aimatch.model.entity.Resume;
import com.aimatch.service.CapabilityGraphService;
import com.aimatch.service.FileService;
import com.aimatch.service.ResumeService;
import com.aimatch.store.DataStores;
import com.aimatch.util.ParsedDataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final DataStores stores;
    private final FileService fileService;
    private final AiParseService aiParseService;
    private final CapabilityGraphService capabilityGraphService;

    @Override
    public Map<String, Object> uploadAndParse(MultipartFile file, Long userId) {
        String text = fileService.extractText(file);
        Map<String, Object> parsedData = aiParseService.parseResume(text);

        Resume resume = buildResumeFromParsedData(parsedData, file, userId);
        resume.setRawText(text);
        resume.setParsedJson(JSONUtil.toJsonStr(parsedData));
        resume.setStatus(1);

        // Deactivate old resumes for this user
        List<Resume> oldResumes = stores.resumeStore.findAll(r ->
            r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
        for (Resume old : oldResumes) {
            old.setStatus(0);
            stores.resumeStore.update(old);
        }

        stores.resumeStore.save(resume);

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("parsedData", parsedData);
        return result;
    }

    @Override
    public Map<String, Object> getResumeDetail(Long resumeId) {
        Resume resume = stores.resumeStore.findById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("name", resume.getName());
        result.put("phone", resume.getPhone());
        result.put("email", resume.getEmail());
        result.put("education", resume.getEducation());
        result.put("school", resume.getSchool());
        result.put("major", resume.getMajor());
        result.put("workYears", resume.getWorkYears());
        result.put("currentCompany", resume.getCurrentCompany());
        result.put("currentPosition", resume.getCurrentPosition());
        result.put("skills", resume.getSkills());
        result.put("certificates", resume.getCertificates());
        result.put("jobIntention", resume.getJobIntention());
        result.put("expectedSalary", resume.getExpectedSalary());
        result.put("expectedCity", resume.getExpectedCity());
        result.put("parsedData", resume.getParsedJson() != null ?
                JSONUtil.parseObj(resume.getParsedJson()) : null);
        return result;
    }

    @Override
    public void updateResume(Long resumeId, Map<String, Object> updateData) {
        Resume resume = stores.resumeStore.findById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }
        if (updateData.containsKey("name")) resume.setName((String) updateData.get("name"));
        if (updateData.containsKey("phone")) resume.setPhone((String) updateData.get("phone"));
        if (updateData.containsKey("email")) resume.setEmail((String) updateData.get("email"));
        if (updateData.containsKey("education")) resume.setEducation((String) updateData.get("education"));
        if (updateData.containsKey("skills")) resume.setSkills((String) updateData.get("skills"));
        if (updateData.containsKey("school")) resume.setSchool((String) updateData.get("school"));
        if (updateData.containsKey("major")) resume.setMajor((String) updateData.get("major"));
        if (updateData.containsKey("workYears") && updateData.get("workYears") != null) resume.setWorkYears(Integer.valueOf(updateData.get("workYears").toString()));
        if (updateData.containsKey("currentCompany")) resume.setCurrentCompany((String) updateData.get("currentCompany"));
        if (updateData.containsKey("currentPosition")) resume.setCurrentPosition((String) updateData.get("currentPosition"));
        if (updateData.containsKey("expectedCity")) resume.setExpectedCity((String) updateData.get("expectedCity"));
        if (updateData.containsKey("expectedSalary")) resume.setExpectedSalary((String) updateData.get("expectedSalary"));
        if (updateData.containsKey("certificates")) resume.setCertificates((String) updateData.get("certificates"));
        if (updateData.containsKey("summary")) resume.setSummary((String) updateData.get("summary"));
        if (updateData.containsKey("jobIntention")) resume.setJobIntention((String) updateData.get("jobIntention"));
        stores.resumeStore.update(resume);
    }

    @Override
    public Map<String, Object> getPersonalCapability(Long userId) {
        List<Resume> resumes = stores.resumeStore.findAll(r -> r.getUserId().equals(userId) && r.getStatus() != null && r.getStatus() == 1);
        if (resumes.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("nodes", List.of());
            empty.put("edges", List.of());
            return empty;
        }
        Resume resume = resumes.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("name", resume.getName());
        result.put("phone", resume.getPhone());
        result.put("email", resume.getEmail());
        result.put("education", resume.getEducation());
        result.put("school", resume.getSchool());
        result.put("major", resume.getMajor());
        result.put("workYears", resume.getWorkYears());
        result.put("currentCompany", resume.getCurrentCompany());
        result.put("currentPosition", resume.getCurrentPosition());
        result.put("expectedCity", resume.getExpectedCity());
        result.put("expectedSalary", resume.getExpectedSalary());
        result.put("jobIntention", resume.getJobIntention());
        result.put("skills", resume.getSkills());
        result.put("certificates", resume.getCertificates());
                result.put("summary", resume.getSummary());
        // Add graph data
        Map<String, Object> graphData = capabilityGraphService.buildPersonCapabilityGraph(userId, resume.getId());
        if (graphData != null) {
                    // Parse parsedJson to parsedData
        String parsedJson = resume.getParsedJson();
        if (parsedJson != null && !parsedJson.isEmpty()) {
            try {
                Object parsedObj = cn.hutool.json.JSONUtil.parse(parsedJson);
                result.put("parsedData", parsedObj);
            } catch (Exception e) {
                log.warn("Failed to parse parsedJson", e);
            }
        }
        result.putAll(graphData);
        }
        return result;
    }

    @Override
    public List<Resume> getResumesByUser(Long userId) {
        return stores.resumeStore.findAll(r -> r.getUserId().equals(userId));
    }

    @Override
    public Map<String, Object> getResumePage(int page, int size) {
        List<Resume> all = stores.resumeStore.findAll(r -> r.getStatus() != null && r.getStatus() == 1);
        all.sort(Comparator.comparing(Resume::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Resume> pageList = from < total ? all.subList(from, to) : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        result.put("current", page);
        result.put("pages", (total + size - 1) / size);
        return result;
    }

    private Resume buildResumeFromParsedData(Map<String, Object> data, MultipartFile file, Long userId) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(getFileType(file.getOriginalFilename()));
        resume.setName(ParsedDataHelper.getString(data, "name"));
        resume.setPhone(ParsedDataHelper.getString(data, "phone"));
        resume.setEmail(ParsedDataHelper.getString(data, "email"));
        resume.setEducation(ParsedDataHelper.getString(data, "education"));
        resume.setSchool(ParsedDataHelper.getString(data, "school"));
        resume.setMajor(ParsedDataHelper.getString(data, "major"));
        resume.setWorkYears(ParsedDataHelper.getInt(data, "workYears"));
        resume.setCurrentCompany(ParsedDataHelper.getString(data, "currentCompany"));
        resume.setCurrentPosition(ParsedDataHelper.getString(data, "currentPosition"));
        resume.setSkills(data.get("skills") != null ? JSONUtil.toJsonStr(data.get("skills")) : null);
        resume.setCertificates(data.get("certificates") != null ? JSONUtil.toJsonStr(data.get("certificates")) : null);
        resume.setLanguages(data.get("languages") != null ? JSONUtil.toJsonStr(data.get("languages")) : null);
        resume.setJobIntention(ParsedDataHelper.getString(data, "jobIntention"));
        resume.setExpectedSalary(ParsedDataHelper.getString(data, "expectedSalary"));
        resume.setExpectedCity(ParsedDataHelper.getString(data, "expectedCity"));
        resume.setWorkExperience(data.get("workExperience") != null ? JSONUtil.toJsonStr(data.get("workExperience")) : null);
        resume.setProjectExperience(data.get("projectExperience") != null ? JSONUtil.toJsonStr(data.get("projectExperience")) : null);
        resume.setSummary(ParsedDataHelper.getString(data, "summary"));
        return resume;
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "word";
        if (lower.endsWith(".txt")) return "txt";
        return "unknown";
    }
}
