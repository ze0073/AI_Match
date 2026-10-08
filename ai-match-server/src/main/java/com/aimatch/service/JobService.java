package com.aimatch.service;

import com.aimatch.model.entity.Job;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface JobService {

    Map<String, Object> uploadAndParse(MultipartFile file, Long hrId);

    Map<String, Object> searchJobs(String keyword, String industry, String city, String education,
                         Integer workYears, int page, int size);

    Map<String, Object> getJobDetail(Long jobId);

    Map<String, Object> getJobCapability(Long jobId);

    void deleteJob(Long jobId);

    void updateJob(Long jobId, Map<String, Object> updateData);
}
