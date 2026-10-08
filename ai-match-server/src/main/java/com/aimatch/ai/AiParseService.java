package com.aimatch.ai;

import java.util.Map;

public interface AiParseService {

    Map<String, Object> parseResume(String rawText);

    Map<String, Object> parseJobDescription(String rawText);

    Map<String, Object> analyzeSkillGap(Map<String, Object> personSkills, Map<String, Object> jobRequirements);

    double calculateMatchScore(Map<String, Object> personSkills, Map<String, Object> jobRequirements);

    /**
     * 生成面试题目（基于职位技能要求）
     */
    String generateInterviewQuestions(String jobTitle, String jobSkills, String resumeSkills);

    /**
     * 评估面试回答
     */
    String evaluateInterviewAnswer(String question, String userAnswer, String jobContext);

    /**
     * 生成学习路线（技能差距分析 + 学习步骤）
     */
    String generateLearningRoadmap(String jobTitle, String jobSkills, String resumeSkills);

}