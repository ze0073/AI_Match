package com.aimatch.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class OpenAiParseService implements AiParseService {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o}")
    private String model;

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_PROMPT_LENGTH = 16000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> parseResume(String rawText) {
        String prompt = buildResumeParsePrompt(truncateText(rawText));
        String response = callAiWithRetry(prompt);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> parseJobDescription(String rawText) {
        String prompt = buildJobParsePrompt(truncateText(rawText));
        String response = callAiWithRetry(prompt);
        return parseJsonResponse(response);
    }

    @Override
    public Map<String, Object> analyzeSkillGap(Map<String, Object> personSkills, Map<String, Object> jobRequirements) {
        String prompt = buildSkillGapPrompt(personSkills, jobRequirements);
        String response = callAiWithRetry(prompt);
        return parseJsonResponse(response);
    }

    @Override
    public double calculateMatchScore(Map<String, Object> personSkills, Map<String, Object> jobRequirements) {
        String prompt = buildMatchScorePrompt(personSkills, jobRequirements);
        String response = callAiWithRetry(prompt);
        Map<String, Object> result = parseJsonResponse(response);
        Object score = result.get("score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        log.warn("AI未返回有效分数，返回0.0, result: {}", result);
        return 0.0;
    }

    private String callAiWithRetry(String prompt) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callAi(prompt);
            } catch (AiServiceException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << attempt);
                    log.warn("AI调用失败，第{}次重试，等待{}ms", attempt + 1, backoff);
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new AiServiceException("AI调用失败，已重试" + MAX_RETRIES + "次", lastException);
    }

    private String callAi(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AiServiceException("AI API Key未配置，请设置OPENAI_API_KEY环境变量");
        }
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 4096);

            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(READ_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String errBody = response.body();
                log.error("AI API返回错误: status={}, body={}", response.statusCode(), errBody);
                throw new AiServiceException("AI API返回错误: HTTP " + response.statusCode());
            }
            JSONObject respJson = JSONUtil.parseObj(response.body());
            return respJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI服务调用异常: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseJsonResponse(String aiResponse) {
        try {
            String json = extractJson(aiResponse);
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.error("AI返回JSON解析失败, response: {}", aiResponse);
            throw new AiServiceException("AI返回内容无法解析为JSON");
        }
    }

    private String extractJson(String text) {
        if (text == null || text.isEmpty()) return "{}";
        String trimmed = text.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        start = trimmed.indexOf("[");
        end = trimmed.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return "{}";
    }

    private String truncateText(String text) {
        if (text == null) return "";
        return text.length() > MAX_PROMPT_LENGTH
                ? text.substring(0, MAX_PROMPT_LENGTH)
                : text;
    }

    private String buildResumeParsePrompt(String rawText) {
        return "请从以下简历文本中提取结构化信息，仅返回JSON格式：\n" +
                "{\n" +
                "  \"name\": \"姓名\",\n" +
                "  \"phone\": \"手机号\",\n" +
                "  \"email\": \"邮箱\",\n" +
                "  \"education\": \"学历\",\n" +
                "  \"school\": \"学校\",\n" +
                "  \"major\": \"专业\",\n" +
                "  \"workYears\": 工作年限数字,\n" +
                "  \"currentCompany\": \"当前公司\",\n" +
                "  \"currentPosition\": \"当前职位\",\n" +
                "  \"skills\": [\"技能1\", \"技能2\"],\n" +
                "  \"workExperience\": [{\"company\":\"\",\"position\":\"\",\"startDate\":\"\",\"endDate\":\"\",\"description\":\"\"}],\n" +
                "  \"projectExperience\": [{\"name\":\"\",\"role\":\"\",\"description\":\"\"}],\n" +
                "  \"expectedSalary\": \"期望薪资\",\n" +
                "  \"expectedCity\": \"期望城市\",\n" +
                "  \"jobIntention\": \"求职意向\",\n" +
                "  \"summary\": \"个人总结\"\n" +
                "}\n\n简历文本：\n" + rawText;
    }

    private String buildJobParsePrompt(String rawText) {
        return "请从以下职位JD文本中提取结构化信息，仅返回JSON格式：\n" +
                "{\n" +
                "  \"title\": \"职位名称\",\n" +
                "  \"department\": \"部门\",\n" +
                "  \"company\": \"公司名称\",\n" +
                "  \"city\": \"城市\",\n" +
                "  \"salary\": \"薪资范围\",\n" +
                "  \"education\": \"学历要求\",\n" +
                "  \"workYears\": 工作年限数字,\n" +
                "  \"industry\": \"行业\",\n" +
                "  \"headCount\": 招聘人数,\n" +
                "  \"description\": \"职位描述\",\n" +
                "  \"requirement\": \"任职要求\",\n" +
                "  \"skillRequirements\": [{\"name\":\"技能名\",\"level\":1-5,\"importance\":1-5}]\n" +
                "}\n\nJD文本：\n" + rawText;
    }

    private String buildSkillGapPrompt(Map<String, Object> personSkills, Map<String, Object> jobRequirements) {
        return "分析以下人才技能与岗位要求的差距，仅返回JSON格式：\n" +
                "{\"matchedSkills\":[\"匹配的技能\"],\"missingSkills\":[\"缺失的技能\"],\"gapAnalysis\":\"差距分析总结\",\"score\":匹配度0-100}\n\n" +
                "人才数据：" + JSONUtil.toJsonStr(personSkills) + "\n" +
                "岗位要求：" + JSONUtil.toJsonStr(jobRequirements);
    }

    private String buildMatchScorePrompt(Map<String, Object> personSkills, Map<String, Object> jobRequirements) {
        return "评估人才与岗位的匹配度，返回0-100的分数和详细分析，仅返回JSON格式：\n" +
                "{\"score\": 匹配分数数字, \"reasons\": [\"匹配理由\"], \"advantages\": [\"优势项\"], \"disadvantages\": [\"短板项\"], \"suggestions\": [\"建议\"]}\n\n" +
                "人才数据：" + JSONUtil.toJsonStr(personSkills) + "\n" +
                "岗位要求：" + JSONUtil.toJsonStr(jobRequirements);
    }

    @Override
    public String generateInterviewQuestions(String jobTitle, String jobSkills, String resumeSkills) {
        String prompt = "你是一位资深技术面试官。请根据以下职位信息和技能要求，生成5道面试题目。\n" +
                "题目应涵盖：2道技术基础题、2道项目经验题、1道场景设计题。\n\n" +
                "职位名称：" + jobTitle + "\n" +
                "职位技能要求：" + jobSkills + "\n" +
                "候选人技能：" + resumeSkills + "\n\n" +
                "请仅返回JSON数组格式，不要添加任何其他文字：\n" +
                "[{\"category\":\"技术基础\",\"question\":\"题目内容\",\"expectedPoints\":\"期望回答要点\"}, ...]";
        return callAiWithRetry(prompt);
    }

    @Override
    public String evaluateInterviewAnswer(String question, String userAnswer, String jobContext) {
        String prompt = "你是一位资深技术面试官。请评估以下面试回答。\n\n" +
                "面试题目：" + question + "\n" +
                "候选人回答：" + truncateText(userAnswer) + "\n" +
                "职位背景：" + jobContext + "\n\n" +
                "请从以下维度评估（满分100分）：\n" +
                "- 技术准确性：回答的技术内容是否正确\n" +
                "- 表达清晰度：是否条理清晰、言简意賅\n" +
                "- 深度与广度：是否展现了足够的深度和相关知识广度\n\n" +
                "仅返回JSON格式，不要添加任何其他文字：\n" +
                "{\"score\": 分数, \"comment\": \"综合评价(50字内)\", \"strengths\": [\"优点\"], \"improvements\": [\"改进建议\"]}";
        return callAiWithRetry(prompt);
    }

    @Override
    public String generateLearningRoadmap(String jobTitle, String jobSkills, String resumeSkills) {
        String prompt = "你是一位资深技术导师。请根据以下职位要求和候选人现有技能，生成一份详细的学习路线图。\n\n" +
                "职位名称：" + jobTitle + "\n" +
                "职位技能要求：" + jobSkills + "\n" +
                "候选人现有技能：" + resumeSkills + "\n\n" +
                "请输出包含以下内容的JSON（仅返回JSON，不要其他文字）：\n" +
                "{\"gapSkills\": [\"缺失技能1\"], \"matchedSkills\": [\"已匹配技能1\"], " +
                "\"roadmap\": [{\"step\": 1, \"title\": \"阶段名称\", \"skills\": [\"需学习技能\"], \"resources\": [\"推荐资源\"], \"duration\": \"预计耗时\"}], " +
                "\"totalDuration\": \"预计总耗时\"}";
        return callAiWithRetry(prompt);
    }
}