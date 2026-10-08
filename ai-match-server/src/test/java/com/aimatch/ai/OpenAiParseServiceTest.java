package com.aimatch.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiParseServiceTest {

    private OpenAiParseService service;

    @BeforeEach
    void setUp() {
        service = new OpenAiParseService();
    }

    @Test
    void parseJsonResponse_shouldHandlePlainJson() throws Exception {
        String json = "{\"name\":\"张三\",\"skills\":[\"Java\",\"Spring\"]}";
        var method = OpenAiParseService.class.getDeclaredMethod("parseJsonResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(service, json);
        assertEquals("张三", result.get("name"));
        assertNotNull(result.get("skills"));
    }

    @Test
    void parseJsonResponse_shouldHandleMarkdownCodeBlock() throws Exception {
        String json = "```json\n{\"name\":\"李四\"}\n```";
        var method = OpenAiParseService.class.getDeclaredMethod("parseJsonResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(service, json);
        assertEquals("李四", result.get("name"));
    }

    @Test
    void parseJsonResponse_shouldHandlePlainCodeBlock() throws Exception {
        String json = "```\n{\"score\":85}\n```";
        var method = OpenAiParseService.class.getDeclaredMethod("parseJsonResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(service, json);
        assertEquals(85, result.get("score"));
    }

    @Test
    void parseJsonResponse_shouldThrowOnBlank() throws Exception {
        var method = OpenAiParseService.class.getDeclaredMethod("parseJsonResponse", String.class);
        method.setAccessible(true);
        try {
            method.invoke(service, "   ");
            fail("Expected AiServiceException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof AiServiceException);
        }
    }
}
