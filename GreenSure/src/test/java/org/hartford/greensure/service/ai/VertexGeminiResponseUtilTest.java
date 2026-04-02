package org.hartford.greensure.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VertexGeminiResponseUtilTest {

    @Test
    void stripMarkdownFences_returnsNullForNull() {
        assertNull(VertexGeminiResponseUtil.stripMarkdownFences(null));
    }

    @Test
    void stripMarkdownFences_leavesPlainJson() {
        String json = "{\"a\":1}";
        assertEquals(json, VertexGeminiResponseUtil.stripMarkdownFences(json));
    }

    @Test
    void stripMarkdownFences_stripsJsonLabeledFence() {
        String wrapped = "```json\n{\"x\":true}\n```";
        assertEquals("{\"x\":true}", VertexGeminiResponseUtil.stripMarkdownFences(wrapped));
    }

    @Test
    void stripMarkdownFences_stripsGenericFence() {
        String wrapped = "```\n{\"y\":2}\n```";
        assertEquals("{\"y\":2}", VertexGeminiResponseUtil.stripMarkdownFences(wrapped));
    }

    @Test
    void extractTextFromGeminiResponseBody_concatenatesMultipleParts() {
        Map<String, Object> part1 = Map.of("text", "hello");
        Map<String, Object> part2 = Map.of("text", "world");
        Map<String, Object> content = Map.of("parts", List.of(part1, part2));
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> body = Map.of("candidates", List.of(candidate));

        assertEquals("hello\nworld", VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(body));
    }

    @Test
    void extractTextFromGeminiResponseBody_returnsNullWhenEmpty() {
        assertNull(VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(null));
        assertNull(VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(Map.of()));
    }

    @Test
    void truncateForLog_shortStringUnchanged() {
        assertEquals("hi", VertexGeminiResponseUtil.truncateForLog("hi", 100));
    }

    @Test
    void truncateForLog_longStringTruncated() {
        String s = "a".repeat(20);
        assertEquals("aaaaaaaaaa… [truncated, 20 chars total]", VertexGeminiResponseUtil.truncateForLog(s, 10));
    }
}
