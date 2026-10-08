package com.aimatch.service;

import com.aimatch.model.entity.Resume;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ResumeService {

    Map<String, Object> uploadAndParse(MultipartFile file, Long userId);

    Map<String, Object> getResumeDetail(Long resumeId);

    void updateResume(Long resumeId, Map<String, Object> updateData);

    Map<String, Object> getPersonalCapability(Long userId);

    List<Resume> getResumesByUser(Long userId);

    Map<String, Object> getResumePage(int page, int size);
}
