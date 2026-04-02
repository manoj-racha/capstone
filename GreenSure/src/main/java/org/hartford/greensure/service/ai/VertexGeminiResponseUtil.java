package org.hartford.greensure.service.ai;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared parsing helpers for Vertex AI and Google AI Studio Gemini REST responses.
 */
@Slf4j
public final class VertexGeminiResponseUtil {

    private VertexGeminiResponseUtil() {
    }

    /**
     * Removes {@code ```} / {@code ```json} wrappers so Jackson can parse the payload.
     */
    public static String stripMarkdownFences(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        while (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1).trim();
            } else {
                t = t.replaceFirst("^```[a-zA-Z0-9_-]*", "").trim();
            }
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end).trim();
            } else {
                break;
            }
        }
        return t.trim();
    }

    /**
     * Extracts model text from {@code generateContent} JSON; concatenates all {@code parts} safely.
     */
    @SuppressWarnings("unchecked")
    public static String extractTextFromGeminiResponseBody(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object promptFeedback = body.get("promptFeedback");
        if (promptFeedback instanceof Map<?, ?> pf) {
            Object block = pf.get("blockReason");
            if (block != null) {
                log.warn("Gemini prompt blocked: {}", block);
            }
        }
        Object error = body.get("error");
        if (error instanceof Map<?, ?> err) {
            Object msg = err.get("message");
            if (msg != null) {
                log.warn("Gemini API error in response body: {}", msg);
            }
        }
        Object candidatesObj = body.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object c0 = candidates.get(0);
        if (!(c0 instanceof Map<?, ?> first)) {
            return null;
        }
        Object finish = first.get("finishReason");
        if (finish != null && "SAFETY".equalsIgnoreCase(String.valueOf(finish))) {
            log.warn("Gemini candidate finishReason=SAFETY; text may be empty");
        }
        Object contentObj = first.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return null;
        }
        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (!(p instanceof Map<?, ?> part)) {
                continue;
            }
            Object text = part.get("text");
            if (text != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(text.toString());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Truncates for safe logging (diagnostics).
     */
    public static String truncateForLog(String s, int maxChars) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "… [truncated, " + s.length() + " chars total]";
    }
}
