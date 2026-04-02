package org.hartford.greensure.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.response.AgentChecklistItem;
import org.hartford.greensure.dto.response.AgentWorkspaceResponse;
import org.hartford.greensure.dto.response.DocumentExtractionResult;
import org.hartford.greensure.enums.MatchStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Vertex AI Gemini via HTTPS (no Vertex Java SDK). Uses ADC from
 * {@link VertexAiAuthService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VertexAiService {

    private static final long MAX_INLINE_BYTES = 4 * 1024 * 1024;
    private static final Set<String> JSON_WRAPPER_KEYS = Set.of(
            "data",
            "result",
            "analysis",
            "response",
            "output",
            "payload",
            "json",
            "document",
            "extraction");

    private final RestTemplate restTemplate;
    private final VertexAiAuthService vertexAiAuthService;
    private final ObjectMapper objectMapper;

    @Value("${vertex.ai.project.id}")
    private String projectId;

    @Value("${vertex.ai.region}")
    private String region;

    @Value("${vertex.ai.model:gemini-2.5-flash}")
    private String vertexModelId;

    @Value("${ai.agent.assistant.enabled:true}")
    private boolean agentAssistantEnabled;

    @Value("${ai.ocr.validator.enabled:true}")
    private boolean ocrValidatorEnabled;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${ai.diagnostic.log.raw.model.response:${ai.diagnostic.log.raw.vertex.response:false}}")
    private boolean logRawModelResponse;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<AgentChecklistItem> generateVerificationChecklist(AgentWorkspaceResponse workspace) {
        try {
            // Guardrails: if AI assistant is disabled/unavailable, keep the agent workflow functional.
            if (!agentAssistantEnabled) {
                return buildFallbackChecklist(workspace);
            }
            Optional<String> tokenOpt = vertexAiAuthService.getAccessToken();
            if (tokenOpt.isEmpty()) {
                return buildFallbackChecklist(workspace);
            }

            String prompt = buildVerificationPrompt(workspace);

            Map<String, Object> generationConfig = vertexJsonGenerationConfig(0.2, 4096);

            Map<String, Object> body = vertexGenerateBody(prompt, generationConfig);

            String url = buildVertexUrl(vertexModelId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenOpt.get());

            // Primary model call.
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

            Long declId = workspace.getDeclarationId();
            if (logRawModelResponse) {
                log.info(
                        "Vertex checklist raw response (declaration {}): {}",
                        declId,
                        VertexGeminiResponseUtil.truncateForLog(String.valueOf(response.getBody()), 12000));
            }

            String jsonText = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(response.getBody());
            if (jsonText == null || jsonText.isBlank()) {
                return buildFallbackChecklist(workspace);
            }
            if (logRawModelResponse) {
                log.info(
                        "Vertex checklist extracted text (declaration {}): {}",
                        declId,
                        VertexGeminiResponseUtil.truncateForLog(jsonText, 12000));
            }
            jsonText = VertexGeminiResponseUtil.stripMarkdownFences(jsonText);

            // Parse defensively because model output can be wrapped/truncated.
            List<Map<String, Object>> rawItems = parseChecklistJson(jsonText, workspace.getDeclarationId());
            if (rawItems == null) {
                return buildFallbackChecklist(workspace);
            }
            List<AgentChecklistItem> items = new ArrayList<>();
            for (Map<String, Object> item : rawItems) {
                if (items.size() >= 6) {
                    break;
                }
                items.add(
                        new AgentChecklistItem(
                                str(item.get("priority")),
                                str(item.get("category")),
                                str(item.get("finding")),
                                str(item.get("action"))));
            }
            return items.isEmpty() ? buildFallbackChecklist(workspace) : items;
        } catch (RestClientException e) {
            log.error("Vertex checklist HTTP error for declaration {}: {}", workspace.getDeclarationId(),
                    e.getMessage());
            return buildFallbackChecklist(workspace);
        } catch (Exception e) {
            log.warn(
                    "Vertex verification checklist failed for declaration {}: {}",
                    workspace.getDeclarationId(),
                    e.getMessage());
            return buildFallbackChecklist(workspace);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DocumentExtractionResult extractFromDocument(String fileUrl, String documentType) {
        try {
            // Runtime toggle to skip OCR without changing controller/service code paths.
            if (!ocrValidatorEnabled) {
                return DocumentExtractionResult.failed("AI OCR validator disabled");
            }
            Optional<String> tokenOpt = vertexAiAuthService.getAccessToken();
            if (tokenOpt.isEmpty()) {
                return DocumentExtractionResult.failed("Vertex AI not authenticated (ADC)");
            }

            // Resolve and validate upload path before sending data to Vertex.
            Path filePath = resolveUploadPath(fileUrl);
            if (!Files.exists(filePath)) {
                log.warn("Document file not found for AI: {}", filePath);
                return DocumentExtractionResult.failed("Uploaded file not found on server");
            }
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length > MAX_INLINE_BYTES) {
                return DocumentExtractionResult.failed("File too large for inline AI (max 4 MB)");
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = detectMimeType(filePath.toString(), fileUrl);
            String prompt = promptForDocumentType(documentType);

            Map<String, Object> generationConfig = vertexJsonGenerationConfig(0.1, 2048);

            Map<String, Object> inline = new LinkedHashMap<>();
            inline.put("mimeType", mimeType);
            inline.put("data", base64);

            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("inlineData", inline);

            Map<String, Object> textPart = new LinkedHashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("role", "user");
            content.put("parts", List.of(imagePart, textPart));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(content));
            body.put("generationConfig", generationConfig);

            String url = buildVertexUrl(vertexModelId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenOpt.get());

            // Multimodal request: inline file bytes + extraction prompt.
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

            if (logRawModelResponse) {
                log.info(
                        "Vertex OCR raw response (type={}, url={}): {}",
                        documentType,
                        fileUrl,
                        VertexGeminiResponseUtil.truncateForLog(String.valueOf(response.getBody()), 12000));
            }

            String jsonText = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(response.getBody());
            if (jsonText == null || jsonText.isBlank()) {
                return DocumentExtractionResult.failed("Empty model response");
            }
            if (logRawModelResponse) {
                log.info(
                        "Vertex OCR extracted text (type={}): {}",
                        documentType,
                        VertexGeminiResponseUtil.truncateForLog(jsonText, 12000));
            }
            jsonText = VertexGeminiResponseUtil.stripMarkdownFences(jsonText);

            // Recover useful fields even when model JSON shape is inconsistent.
            Map<String, Object> map = parseDocumentJson(jsonText, fileUrl, documentType);
            if (map == null || map.isEmpty()) {
                return DocumentExtractionResult.failed("AI extraction returned invalid JSON");
            }
            return mapToExtractionResult(map, documentType);
        } catch (RestClientException e) {
            log.error("Vertex document extract HTTP error for {}: {}", fileUrl, e.getMessage());
            return DocumentExtractionResult.failed("AI extraction failed — manual review needed");
        } catch (Exception e) {
            log.error("Vertex document extract failed for {}: {}", fileUrl, e.getMessage(), e);
            return DocumentExtractionResult.failed("AI extraction failed — manual review needed");
        }
    }

    /**
     * Parses OCR JSON defensively. Gemini may return object, single-item array, or
     * wrapped text slices.
     */
    private Map<String, Object> parseDocumentJson(String jsonText, String fileUrl, String documentType) {
        String t = jsonText == null ? "" : jsonText.trim();
        if (t.isEmpty()) {
            return null;
        }

        // Try multiple candidate slices because models often add wrappers/text around JSON.
        List<String> candidates = new ArrayList<>();
        candidates.add(t);

        String objBalanced = extractBalancedJsonObject(t);
        if (objBalanced != null && !objBalanced.equals(t)) {
            candidates.add(objBalanced);
        }
        String arrBalanced = extractBalancedJsonArray(t);
        if (arrBalanced != null && !arrBalanced.equals(t)) {
            candidates.add(arrBalanced);
        }

        int objFirst = t.indexOf('{');
        int objLast = t.lastIndexOf('}');
        if (objFirst >= 0 && objLast > objFirst) {
            String slice = t.substring(objFirst, objLast + 1);
            if (!candidates.contains(slice)) {
                candidates.add(slice);
            }
        }

        for (String candidate : candidates) {
            try {
                JsonNode root = objectMapper.readTree(candidate);
                if (root == null || root.isNull()) {
                    continue;
                }
                if (root.isTextual()) {
                    // Some responses embed JSON as an escaped string; recurse once more.
                    String nested = root.asText();
                    if (nested != null && !nested.isBlank() && !nested.equals(candidate)) {
                        Map<String, Object> nestedMap = parseDocumentJson(nested, fileUrl, documentType);
                        if (nestedMap != null && !nestedMap.isEmpty()) {
                            return nestedMap;
                        }
                    }
                    continue;
                }

                root = unwrapJsonNode(root);

                if (root.isObject()) {
                    Map<String, Object> objectMap = objectMapper.convertValue(root, new TypeReference<>() {
                    });
                    Map<String, Object> normalized = normalizeExtractionMap(objectMap);
                    if (normalized != null && !normalized.isEmpty()) {
                        return normalized;
                    }
                }

                if (root.isArray() && !root.isEmpty()) {
                    Map<String, Object> fromArray = normalizeArrayExtraction(root);
                    if (fromArray != null && !fromArray.isEmpty()) {
                        return fromArray;
                    }
                }
            } catch (Exception e) {
                log.debug(
                        "Vertex OCR parse attempt failed (type={}, url={}): {}",
                        documentType,
                        fileUrl,
                        e.getMessage());
            }
        }

        log.warn(
                "Vertex OCR parse could not recover JSON (type={}, url={})",
                documentType,
                fileUrl);
        return null;
    }

    private static JsonNode unwrapJsonNode(JsonNode node) {
        JsonNode current = node;
        while (current != null) {
            if (current.isArray() && current.size() == 1) {
                current = current.get(0);
                continue;
            }
            if (current.isObject() && current.size() == 1) {
                String field = current.fieldNames().next();
                if (field != null && JSON_WRAPPER_KEYS.contains(field.toLowerCase(Locale.ROOT))) {
                    current = current.get(field);
                    continue;
                }
            }
            break;
        }
        return current;
    }

    private Map<String, Object> normalizeArrayExtraction(JsonNode arrayNode) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (JsonNode item : arrayNode) {
            JsonNode normalizedItem = unwrapJsonNode(item);
            if (!normalizedItem.isObject()) {
                continue;
            }
            Map<String, Object> itemMap = objectMapper.convertValue(normalizedItem, new TypeReference<>() {
            });

            String key = firstString(itemMap, "field", "name", "key", "label");
            Object value = firstNonNull(itemMap, "value", "extractedValue", "extracted_value", "text");
            if (key != null && value != null) {
                merged.put(key, value);
            } else {
                merged.putAll(itemMap);
            }
        }
        return normalizeExtractionMap(merged);
    }

    private Map<String, Object> normalizeExtractionMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }

        Map<String, Object> current = map;
        // Peel known wrapper keys (data/result/response/...) to get the actual payload object.
        while (current.size() == 1) {
            Object next = firstNonNull(current,
                    "data",
                    "result",
                    "analysis",
                    "response",
                    "output",
                    "payload",
                    "json",
                    "document",
                    "extraction");
            if (!(next instanceof Map<?, ?> m)) {
                break;
            }
            current = castMap(m);
        }

        Object fields = firstNonNull(current, "fields", "values", "items");
        if (fields instanceof List<?> list) {
            // Flatten [{field, value}] style outputs into a direct key-value map.
            Map<String, Object> flattened = new LinkedHashMap<>(current);
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }
                Map<String, Object> fm = castMap(m);
                String key = firstString(fm, "field", "name", "key", "label");
                Object value = firstNonNull(fm, "value", "extractedValue", "extracted_value", "text");
                if (key != null && value != null) {
                    flattened.put(key, value);
                }
            }
            return flattened;
        }

        return current;
    }

    private static String firstString(Map<String, Object> map, String... keys) {
        Object v = firstNonNull(map, keys);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            out.put(entry.getKey().toString(), entry.getValue());
        }
        return out;
    }

    private DocumentExtractionResult mapToExtractionResult(Map<String, Object> map, String documentType) {
        Double conf = doubleOrNull(firstNonNull(map, "confidence"));
        if (conf == null) {
            conf = 0.5;
        }
        DocumentExtractionResult.DocumentExtractionResultBuilder b = DocumentExtractionResult.builder();
        b.success(true);
        b.billingMonth(strNullable(firstNonNull(map, "billingMonth", "billing_month")));
        b.unitsKwh(doubleOrNull(firstNonNull(map, "unitsKwh", "units_kwh")));
        b.amount(doubleOrNull(firstNonNull(map, "amount")));
        b.consumerNumber(strNullable(firstNonNull(map, "consumerNumber", "consumer_number")));
        b.providerName(strNullable(firstNonNull(map, "providerName", "provider_name")));
        b.distributorName(strNullable(firstNonNull(map, "distributorName", "distributor_name")));
        b.registrationNumber(strNullable(firstNonNull(map, "registrationNumber", "registration_number")));
        b.fuelType(strNullable(firstNonNull(map, "fuelType", "fuel_type")));
        b.scmConsumed(doubleOrNull(firstNonNull(map, "scmConsumed", "scm_consumed")));
        b.installedCapacityKw(doubleOrNull(firstNonNull(map, "installedCapacityKw", "installed_capacity_kw")));
        b.installationDate(strNullable(firstNonNull(map, "installationDate", "installation_date")));
        b.cylinderCount(intOrNull(firstNonNull(map, "cylinderCount", "cylinder_count")));
        b.confidence(conf);
        List<String> anomalies = new ArrayList<>();
        Object a = firstNonNull(map, "anomalies");
        if (a instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    anomalies.add(o.toString());
                }
            }
        }
        b.anomalies(anomalies);
        return b.build();
    }

    private static Object firstNonNull(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Double doubleOrNull(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intOrNull(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String strNullable(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Path resolveUploadPath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return Paths.get(uploadDir);
        }
        String u = fileUrl.trim();
        if (u.startsWith("http://localhost:9090")) {
            u = u.substring("http://localhost:9090".length());
        }
        if (u.startsWith("http://127.0.0.1:9090")) {
            u = u.substring("http://127.0.0.1:9090".length());
        }
        String rel = u.startsWith("/") ? u.substring(1) : u;
        if (rel.startsWith("uploads/")) {
            rel = rel.substring("uploads/".length());
        }
        Path resolved = Paths.get(uploadDir).resolve(rel).normalize();
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!resolved.toAbsolutePath().normalize().startsWith(base)) {
            return base;
        }
        return resolved;
    }

    private static String detectMimeType(String path, String url) {
        String p = (path + " " + url).toLowerCase(Locale.ROOT);
        if (p.endsWith(".pdf") || p.contains(".pdf")) {
            return "application/pdf";
        }
        if (p.contains(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private static String promptForDocumentType(String documentType) {
        return switch (documentType) {
            case "RC_BOOK", "INSURANCE", "POLLUTION_CERTIFICATE" ->
                """
                        Extract fields from this Indian vehicle document. Return ONLY valid JSON:
                        {
                          "registrationNumber": "string",
                          "fuelType": "string",
                          "confidence": number between 0 and 1,
                          "anomalies": ["list of concerns"]
                        }
                        If a field cannot be found, use null.
                        """;
            case "LPG_RECEIPT" ->
                """
                        Extract fields from this Indian LPG booking receipt. Return ONLY valid JSON:
                        {
                          "bookingDate": "YYYY-MM-DD",
                          "cylinderCount": number,
                          "consumerName": "string",
                          "distributorName": "string",
                          "confidence": number between 0 and 1,
                          "anomalies": ["list of concerns"]
                        }
                        If a field cannot be found, use null.
                        """;
            case "PNG_BILL" ->
                """
                        Extract fields from this Indian piped natural gas (PNG) bill. Return ONLY valid JSON:
                        {
                          "billingMonth": "YYYY-MM",
                          "scmConsumed": number,
                          "amount": number,
                          "consumerNumber": "string",
                          "confidence": number between 0 and 1,
                          "anomalies": ["list of concerns"]
                        }
                        If a field cannot be found, use null.
                        """;
            case "SOLAR_CERTIFICATE" ->
                """
                        Extract fields from this solar panel installation certificate. Return ONLY valid JSON:
                        {
                          "installedCapacityKw": number,
                          "installationDate": "YYYY-MM-DD",
                          "installerName": "string",
                          "consumerAddress": "string",
                          "confidence": number between 0 and 1,
                          "anomalies": ["list of concerns"]
                        }
                        If a field cannot be found, use null.
                        """;
            default ->
                """
                        Extract the following fields from this Indian electricity bill. Return ONLY valid JSON.
                        No markdown. No explanation.
                        {
                          "billingMonth": "YYYY-MM",
                          "unitsKwh": number,
                          "amount": number,
                          "consumerNumber": "string",
                          "providerName": "string",
                          "confidence": number between 0 and 1,
                          "anomalies": ["list of concern strings"]
                        }
                        If a field cannot be found, use null.
                        """;
        };
    }

    private static Map<String, Object> vertexJsonGenerationConfig(double temperature, int maxOutputTokens) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", "application/json");
        return generationConfig;
    }

    private Map<String, Object> vertexGenerateBody(String promptText, Map<String, Object> generationConfig) {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(textPart));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private String buildVertexUrl(String model) {
        return String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                region,
                projectId,
                region,
                model);
    }

    /**
     * Parses the checklist JSON array; retries with a balanced {@code [...]} slice
     * or first-to-last bracket slice when
     * the model wraps or truncates output.
     */
    private List<Map<String, Object>> parseChecklistJson(String jsonText, Long declarationId) {
        String t = jsonText == null ? "" : jsonText.trim();
        if (t.isEmpty()) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(t);
        String balanced = extractBalancedJsonArray(t);
        if (balanced != null && !balanced.equals(t)) {
            candidates.add(balanced);
        }
        int first = t.indexOf('[');
        int last = t.lastIndexOf(']');
        if (first >= 0 && last > first) {
            String slice = t.substring(first, last + 1);
            if (!candidates.contains(slice)) {
                candidates.add(slice);
            }
        }
        for (String candidate : candidates) {
            try {
                List<Map<String, Object>> list = objectMapper.readValue(candidate, new TypeReference<>() {
                });
                if (list != null) {
                    return list;
                }
            } catch (Exception e) {
                log.debug(
                        "Checklist JSON parse attempt failed for declaration {}: {}",
                        declarationId,
                        e.getMessage());
            }
        }
        return null;
    }

    /**
     * Returns the substring from the first {@code [} through the matching
     * {@code ]}, respecting JSON string escapes.
     */
    private static String extractBalancedJsonArray(String s) {
        int start = s.indexOf('[');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Returns the substring from the first
     * {@code \{} through the matching {@code \}}, respecting JSON string escapes.
     */
    private static String extractBalancedJsonObject(String s) {
        int start = s.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private String buildVerificationPrompt(AgentWorkspaceResponse w) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                        You are an expert insurance fraud investigator and carbon verification specialist in India.

                        An agent is about to physically visit a household to verify their carbon declaration. Analyse all
                        the data below and produce a prioritised checklist of specific things the agent must check during the visit.

                        Return ONLY a JSON array. No explanation text. No markdown. Only valid JSON.

                        finding and action must be short single-line plain text. Do not put double-quote characters inside
                        finding or action strings (rephrase if needed). This keeps the JSON valid.

                        JSON format:
                        [
                          {
                            "priority": "HIGH",
                            "category": "VEHICLE",
                            "finding": "specific finding description",
                            "action": "specific action for agent to take"
                          }
                        ]

                        Priority must be HIGH, MEDIUM, or LOW.
                        Category must be one of: VEHICLE, ELECTRICITY, COOKING, SOLAR, HOUSEHOLD, GENERAL.
                        Maximum 6 items. Order by priority descending.
                        Only include items that need attention.
                        Do not include items that are fine.

                        DECLARATION DATA:

                        Household:
                        Members: %s
                        Address: %s
                        Pincode: %s

                        Fraud Advisory:
                        Risk Level: %s
                        Fraud Score: %s
                        Fired Rules: %s

                        """
                        .formatted(
                                w.getHouseholdMemberCount() != null ? w.getHouseholdMemberCount() : "unknown",
                                nz(w.getUserAddress()),
                                nz(w.getPinCode()),
                                nz(w.getFraudRiskLevel()),
                                w.getFraudScore() != null ? w.getFraudScore() : 0,
                                w.getFraudFlags() != null ? String.join(", ", w.getFraudFlags()) : "(none)"));

        sb.append("Vehicle Comparisons:\n");
        if (w.getVehicles() != null) {
            for (int i = 0; i < w.getVehicles().size(); i++) {
                AgentWorkspaceResponse.VehicleComparisonBlock v = w.getVehicles().get(i);
                sb.append("  Vehicle ").append(i + 1).append(": ").append(nz(v.getVehicleLabel())).append("\n");
                if (v.getComparisons() != null) {
                    for (AgentWorkspaceResponse.ComparisonField f : v.getComparisons()) {
                        sb.append(comparisonLine(f));
                    }
                }
            }
        }
        sb.append("\nElectricity Comparisons:\n");
        if (w.getElectricityComparison() != null) {
            for (AgentWorkspaceResponse.ComparisonField f : w.getElectricityComparison()) {
                sb.append(comparisonLine(f));
            }
        }
        sb.append("\nCooking Comparisons:\n");
        if (w.getCookingComparison() != null) {
            for (AgentWorkspaceResponse.ComparisonField f : w.getCookingComparison()) {
                sb.append(comparisonLine(f));
            }
        }
        sb.append("\nSolar Comparisons:\n");
        if (w.getSolarComparison() != null) {
            for (AgentWorkspaceResponse.ComparisonField f : w.getSolarComparison()) {
                sb.append(comparisonLine(f));
            }
        }
        sb.append("\nSummary comparison table:\n");
        if (w.getComparisonTable() != null) {
            for (AgentWorkspaceResponse.ComparisonField f : w.getComparisonTable()) {
                sb.append(comparisonLine(f));
            }
        }
        sb.append(
                String.format(
                        Locale.ROOT,
                        "\nPreliminary signals — declared monthly kWh (user): %s, OCR avg kWh: %s, bills uploaded: %s\n",
                        w.getUserDeclaredMonthlyKwh() != null ? w.getUserDeclaredMonthlyKwh() : "n/a",
                        w.getOcrComputedMonthlyKwh() != null ? w.getOcrComputedMonthlyKwh() : "n/a",
                        w.getBillsUploaded() != null ? w.getBillsUploaded() : 0));
        return sb.toString();
    }

    private static String comparisonLine(AgentWorkspaceResponse.ComparisonField f) {
        if (f == null) {
            return "";
        }
        return String.format(
                Locale.ROOT,
                "  - %s: User=%s, System=%s, Status=%s, Note=%s%n",
                nz(f.getFieldName()),
                nz(f.getUserClaim()),
                nz(f.getSystemValue()),
                f.getMatchStatus() != null ? f.getMatchStatus().name() : "—",
                f.getNote() != null ? f.getNote() : "");
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private List<AgentChecklistItem> buildFallbackChecklist(AgentWorkspaceResponse w) {
        // Deterministic fallback keeps agent UX usable when model output is unavailable.
        List<AgentChecklistItem> items = new ArrayList<>();
        if ("HIGH".equalsIgnoreCase(nz(w.getFraudRiskLevel()))) {
            items.add(
                    new AgentChecklistItem(
                            "HIGH",
                            "GENERAL",
                            "High fraud risk detected (score " + (w.getFraudScore() != null ? w.getFraudScore() : 0)
                                    + ")",
                            "Carefully verify all declared values on site"));
        }
        addMismatches(w.getComparisonTable(), items, "GENERAL");
        if (w.getElectricityComparison() != null) {
            addMismatches(w.getElectricityComparison(), items, "ELECTRICITY");
        }
        if (w.getCookingComparison() != null) {
            addMismatches(w.getCookingComparison(), items, "COOKING");
        }
        if (w.getSolarComparison() != null) {
            addMismatches(w.getSolarComparison(), items, "SOLAR");
        }
        if (w.getVehicles() != null) {
            for (AgentWorkspaceResponse.VehicleComparisonBlock vb : w.getVehicles()) {
                if (vb.getComparisons() != null) {
                    addMismatches(vb.getComparisons(), items, "VEHICLE");
                }
            }
        }
        return items.size() > 6 ? items.subList(0, 6) : items;
    }

    private void addMismatches(
            List<AgentWorkspaceResponse.ComparisonField> fields,
            List<AgentChecklistItem> items,
            String category) {
        if (fields == null) {
            return;
        }
        for (AgentWorkspaceResponse.ComparisonField f : fields) {
            if (items.size() >= 6) {
                return;
            }
            if (f.getMatchStatus() == MatchStatus.MISMATCH) {
                items.add(
                        new AgentChecklistItem(
                                "HIGH",
                                category,
                                f.getFieldName()
                                        + " mismatch: user claims "
                                        + nz(f.getUserClaim())
                                        + ", system shows "
                                        + nz(f.getSystemValue()),
                                "Confirm actual value on site"));
            }
        }
    }
}
