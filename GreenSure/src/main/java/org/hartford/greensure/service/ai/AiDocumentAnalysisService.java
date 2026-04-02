package org.hartford.greensure.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.response.AgentChecklistItem;
import org.hartford.greensure.dto.response.AiCookingAnalysis;
import org.hartford.greensure.dto.response.AiDocumentAnalysisResult;
import org.hartford.greensure.dto.response.AiElectricityAnalysis;
import org.hartford.greensure.dto.response.AiSolarAnalysis;
import org.hartford.greensure.dto.response.AiVehicleAnalysis;
import org.hartford.greensure.dto.response.DocumentExtractionResult;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.CookingData;
import org.hartford.greensure.entity.DeclarationVehicleData;
import org.hartford.greensure.entity.ElectricityBill;
import org.hartford.greensure.entity.ElectricityData;
import org.hartford.greensure.entity.HouseholdProfile;
import org.hartford.greensure.entity.SolarData;
import org.hartford.greensure.entity.VehicleDocument;
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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiDocumentAnalysisService {

    private static final long MAX_INLINE_BYTES = 4L * 1024L * 1024L;
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",\\s*([}\\]])");

    /** Unwrap {@code {"result":{...}}} style payloads from the model. */
    private static final Set<String> JSON_WRAPPER_KEYS = Set.of(
            "data",
            "result",
            "analysis",
            "response",
            "output",
            "declaration",
            "payload",
            "report",
            "verification",
            "json");

    private final RestTemplate restTemplate;
    private final VertexAiAuthService vertexAiAuthService;
    private final VertexAiService vertexAiService;
    private final ObjectMapper objectMapper;

    @Value("${vertex.ai.project.id}")
    private String projectId;

    @Value("${vertex.ai.region}")
    private String region;

    @Value("${vertex.ai.model:gemini-2.5-flash}")
    private String vertexModelId;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${ai.request.retries:1}")
    private int aiRequestRetries;

    @Value("${ai.request.retry.backoff.ms:1200}")
    private long aiRequestRetryBackoffMs;

    /**
     * Prefer {@code ai.diagnostic.log.raw.model.response}; legacy
     * {@code ai.diagnostic.log.raw.vertex.response} still works.
     */
    @Value("${ai.diagnostic.log.raw.model.response:${ai.diagnostic.log.raw.vertex.response:false}}")
    private boolean logRawModelResponse;

    record DocumentPart(String label, String base64Data, String mimeType) {
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDocumentAnalysisResult analyseDeclarationDocuments(
            Long declarationId,
            CarbonDeclaration declaration,
            List<DeclarationVehicleData> vehicles,
            ElectricityData electricityData,
            List<ElectricityBill> electricityBills,
            CookingData cookingData,
            SolarData solarData,
            HouseholdProfile householdProfile) {

        try {
            Optional<String> tokenOpt = vertexAiAuthService.getAccessToken();
            if (tokenOpt.isEmpty()) {
                return failedResult("Vertex AI authentication failed", LocalDateTime.now());
            }

            List<DocumentPart> documentParts = new ArrayList<>();
            List<String> skippedNotes = new ArrayList<>();

            collectElectricityBills(electricityBills, documentParts, skippedNotes);
            collectVehicleDocuments(vehicles, documentParts, skippedNotes);
            collectCookingDocuments(cookingData, documentParts, skippedNotes);
            collectSolarCertificate(solarData, documentParts, skippedNotes);

            if (documentParts.isEmpty()) {
                return AiDocumentAnalysisResult.builder()
                        .analysisSuccess(false)
                        .errorMessage("No readable documents available for AI analysis")
                        .overallFindings(new ArrayList<>())
                        .analysedAt(LocalDateTime.now())
                        .build();
            }

            String prompt = buildPrompt(
                    declaration,
                    vehicles,
                    electricityData,
                    electricityBills,
                    cookingData,
                    solarData,
                    householdProfile,
                    documentParts,
                    skippedNotes);

            Map<String, Object> body = buildRequestBody(documentParts, prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenOpt.get());

            String vertexUrl = buildVertexUrl(vertexModelId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = postForEntityWithRetry(vertexUrl, entity, declarationId);

            if (logRawModelResponse) {
                log.info(
                        "Vertex raw response (declaration {}): {}",
                        declarationId,
                        VertexGeminiResponseUtil.truncateForLog(String.valueOf(response.getBody()), 12000));
            } else {
                log.debug(
                        "Vertex raw response body keys (declaration {}): {}",
                        declarationId,
                        response.getBody() != null ? response.getBody().keySet() : "null");
            }

            String responseText = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(response.getBody());
            if (responseText == null || responseText.isBlank()) {
                return failedResult("AI response was empty", LocalDateTime.now());
            }

            if (logRawModelResponse) {
                log.info(
                        "Vertex extracted model text (declaration {}): {}",
                        declarationId,
                        VertexGeminiResponseUtil.truncateForLog(responseText, 12000));
            }

            responseText = VertexGeminiResponseUtil.stripMarkdownFences(responseText);

            ObjectMapper safeMapper = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    // Gemini often omits fields or sends null; Jackson 2.18+ defaults this to true
                    // and breaks primitives.
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

            AiDocumentAnalysisResult parsed = tryParseFullResult(safeMapper, responseText, declarationId);
            if (parsed == null) {
                AiDocumentAnalysisResult fallback = tryParsePartialResult(safeMapper, responseText, declarationId);
                if (fallback == null) {
                    String strictResponse = runStrictStructuredRetry(
                            vertexUrl,
                            headers,
                            documentParts,
                            prompt,
                            declarationId);
                    if (strictResponse != null && !strictResponse.isBlank()) {
                        String strictCleaned = VertexGeminiResponseUtil.stripMarkdownFences(strictResponse);
                        parsed = tryParseFullResult(safeMapper, strictCleaned, declarationId);
                        if (parsed == null) {
                            fallback = tryParsePartialResult(safeMapper, strictCleaned, declarationId);
                        }
                    }
                }

                if (fallback == null && parsed == null) {
                    String repairedResponse = attemptJsonRepairWithModel(responseText, headers, declarationId);
                    if (repairedResponse != null && !repairedResponse.isBlank()) {
                        String cleanedRepaired = VertexGeminiResponseUtil.stripMarkdownFences(repairedResponse);
                        parsed = tryParseFullResult(safeMapper, cleanedRepaired, declarationId);
                        if (parsed == null) {
                            fallback = tryParsePartialResult(safeMapper, cleanedRepaired, declarationId);
                        }
                    }
                }

                if (parsed != null) {
                    parsed.setAnalysisSuccess(true);
                    parsed.setErrorMessage(null);
                    parsed.setAnalysedAt(LocalDateTime.now());
                    if (parsed.getVehicles() == null) {
                        parsed.setVehicles(new ArrayList<>());
                    }
                    if (parsed.getOverallFindings() == null) {
                        parsed.setOverallFindings(new ArrayList<>());
                    }
                    return enrichWithPerDocumentOcr(
                            parsed,
                            vehicles,
                            electricityData,
                            electricityBills,
                            cookingData);
                }

                if (fallback != null) {
                    fallback.setAnalysisSuccess(true);
                    fallback.setErrorMessage(null);
                    fallback.setAnalysedAt(LocalDateTime.now());
                    if (fallback.getVehicles() == null) {
                        fallback.setVehicles(new ArrayList<>());
                    }
                    if (fallback.getOverallFindings() == null) {
                        fallback.setOverallFindings(new ArrayList<>());
                    }
                    return enrichWithPerDocumentOcr(
                            fallback,
                            vehicles,
                            electricityData,
                            electricityBills,
                            cookingData);
                }

                List<AgentChecklistItem> fallbackFindings = extractOverallFindingsOnly(safeMapper, responseText,
                        declarationId);
                log.warn(
                        "AI document analysis: could not parse JSON for declaration {}. Response prefix: {}",
                        declarationId,
                        VertexGeminiResponseUtil.truncateForLog(responseText, 1200));
                return buildGracefulManualFallbackResult(
                        fallbackFindings,
                        skippedNotes,
                        vehicles,
                        electricityData,
                        cookingData,
                        solarData,
                        LocalDateTime.now());
            }

            parsed.setAnalysisSuccess(true);
            parsed.setErrorMessage(null);
            parsed.setAnalysedAt(LocalDateTime.now());
            if (parsed.getVehicles() == null) {
                parsed.setVehicles(new ArrayList<>());
            }
            if (parsed.getOverallFindings() == null) {
                parsed.setOverallFindings(new ArrayList<>());
            }
            return enrichWithPerDocumentOcr(
                    parsed,
                    vehicles,
                    electricityData,
                    electricityBills,
                    cookingData);
        } catch (RestClientException e) {
            log.error("AI document analysis HTTP failed for declaration {}: {}", declarationId, e.getMessage(), e);
            if (isRetryableTimeout(e)) {
                return failedResult("AI analysis timed out while contacting Vertex AI. Please retry.",
                        LocalDateTime.now());
            }
            return failedResult("AI analysis failed: " + e.getMessage(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("AI document analysis failed for declaration {}: {}", declarationId, e.getMessage(), e);
            return failedResult("AI analysis failed: " + e.getMessage(), LocalDateTime.now());
        }
    }

    private void collectElectricityBills(
            List<ElectricityBill> bills,
            List<DocumentPart> documentParts,
            List<String> skippedNotes) {
        if (bills == null) {
            return;
        }
        for (ElectricityBill bill : bills) {
            String url = bill.getBillUrl();
            String month = bill.getBillingMonth() != null ? bill.getBillingMonth() : "unknown-month";
            addDocumentPart(
                    "Electricity bill - " + month,
                    url,
                    documentParts,
                    skippedNotes,
                    "Electricity bill " + month);
        }
    }

    private void collectVehicleDocuments(
            List<DeclarationVehicleData> vehicles,
            List<DocumentPart> documentParts,
            List<String> skippedNotes) {
        if (vehicles == null) {
            return;
        }
        for (DeclarationVehicleData vehicle : vehicles) {
            String reg = vehicle.getRegistrationNumber() != null ? vehicle.getRegistrationNumber() : "UNKNOWN-REG";
            if (vehicle.getDocuments() == null) {
                continue;
            }
            for (VehicleDocument doc : vehicle.getDocuments()) {
                String type = doc.getDocumentType() != null ? doc.getDocumentType().name() : "UNKNOWN";
                addDocumentPart(
                        "Vehicle doc - " + reg + " - " + type,
                        doc.getDocumentUrl(),
                        documentParts,
                        skippedNotes,
                        "Vehicle document " + reg + " (" + type + ")");
            }
        }
    }

    private void collectCookingDocuments(
            CookingData cookingData,
            List<DocumentPart> documentParts,
            List<String> skippedNotes) {
        if (cookingData == null || cookingData.getBillUrls() == null || cookingData.getBillUrls().isBlank()) {
            return;
        }

        List<String> urls = parseUrlList(cookingData.getBillUrls());
        for (int i = 0; i < urls.size(); i++) {
            String label = "Cooking document " + (i + 1);
            addDocumentPart(label, urls.get(i), documentParts, skippedNotes, label);
        }
    }

    private void collectSolarCertificate(
            SolarData solarData,
            List<DocumentPart> documentParts,
            List<String> skippedNotes) {
        if (solarData == null || solarData.getCertificateUrl() == null || solarData.getCertificateUrl().isBlank()) {
            return;
        }
        addDocumentPart(
                "Solar installation certificate",
                solarData.getCertificateUrl(),
                documentParts,
                skippedNotes,
                "Solar installation certificate");
    }

    private void addDocumentPart(
            String label,
            String fileUrl,
            List<DocumentPart> documentParts,
            List<String> skippedNotes,
            String fallbackName) {

        if (fileUrl == null || fileUrl.isBlank()) {
            skippedNotes.add(fallbackName + " has empty URL");
            return;
        }

        try {
            String mimeType = detectMimeType(fileUrl);
            if ("application/octet-stream".equals(mimeType)) {
                skippedNotes.add(fallbackName + " has unsupported format");
                return;
            }

            byte[] bytes = readFileFromUrl(fileUrl);
            if (bytes.length > MAX_INLINE_BYTES) {
                skippedNotes.add(fallbackName + " is larger than 4MB and was skipped");
                return;
            }

            documentParts.add(new DocumentPart(label, base64Encode(bytes), mimeType));
        } catch (IOException e) {
            log.warn("Could not read file for AI analysis: {} ({})", fileUrl, e.getMessage());
            skippedNotes.add(fallbackName + " could not be read from disk");
        } catch (Exception e) {
            log.warn("Could not prepare file for AI analysis: {} ({})", fileUrl, e.getMessage());
            skippedNotes.add(fallbackName + " could not be prepared for AI analysis");
        }
    }

    private Map<String, Object> buildRequestBody(List<DocumentPart> documentParts, String prompt) {
        return buildRequestBody(documentParts, prompt, false);
    }

    private Map<String, Object> buildRequestBody(
            List<DocumentPart> documentParts,
            String prompt,
            boolean strictSchemaMode) {
        List<Map<String, Object>> parts = new ArrayList<>();

        for (DocumentPart documentPart : documentParts) {
            Map<String, Object> inlineData = new LinkedHashMap<>();
            inlineData.put("mimeType", documentPart.mimeType());
            inlineData.put("data", documentPart.base64Data());

            Map<String, Object> inlinePart = new LinkedHashMap<>();
            inlinePart.put("inlineData", inlineData);
            parts.add(inlinePart);
        }

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> generationConfig = strictSchemaMode
                ? vertexJsonGenerationConfig(0.0, 5000, true)
                : vertexJsonGenerationConfig(0.1, 4000, false);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);
        return requestBody;
    }

    private String runStrictStructuredRetry(
            String vertexUrl,
            HttpHeaders headers,
            List<DocumentPart> documentParts,
            String originalPrompt,
            Long declarationId) {
        try {
            String strictPrompt = """
                    Return ONLY a valid JSON object with top-level keys exactly:
                    electricity, vehicles, cooking, solar, overallFindings.
                    Do not include markdown or explanations.
                    If any field is unavailable, use null/false/0/[] as appropriate.

                    Context and instructions:
                    %s
                    """.formatted(originalPrompt);

            Map<String, Object> strictBody = buildRequestBody(documentParts, strictPrompt, true);
            ResponseEntity<Map> strictResponse = postForEntityWithRetry(
                    vertexUrl,
                    new HttpEntity<>(strictBody, headers),
                    declarationId);
            String strictText = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(strictResponse.getBody());
            if (logRawModelResponse && strictText != null) {
                log.info(
                        "Vertex strict retry text (declaration {}): {}",
                        declarationId,
                        VertexGeminiResponseUtil.truncateForLog(strictText, 12000));
            }
            return strictText;
        } catch (Exception e) {
            log.debug("Strict structured retry failed for declaration {}: {}", declarationId, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(
            CarbonDeclaration declaration,
            List<DeclarationVehicleData> vehicles,
            ElectricityData electricityData,
            List<ElectricityBill> electricityBills,
            CookingData cookingData,
            SolarData solarData,
            HouseholdProfile householdProfile,
            List<DocumentPart> documentParts,
            List<String> skippedNotes) {

        String electricityProvider = electricityData != null ? safe(electricityData.getProvider()) : "Not provided";
        String electricityConsumer = electricityData != null ? safe(electricityData.getConsumerNumber())
                : "Not provided";
        String electricityKwh = electricityData != null && electricityData.getUserDeclaredMonthlyKwh() != null
                ? String.valueOf(electricityData.getUserDeclaredMonthlyKwh())
                : "Not provided";

        int billCount = electricityBills != null ? electricityBills.size() : 0;

        String cookingFuelType = cookingData != null && cookingData.getFuelType() != null
                ? cookingData.getFuelType().name()
                : "Not provided";

        String solarFlag = (solarData != null && solarData.isHasSolar()) ? "YES" : "NO";

        String address = "Not provided";
        String city = "Not provided";
        String state = "Not provided";
        String pin = "Not provided";
        if (householdProfile != null && householdProfile.getUser() != null) {
            var u = householdProfile.getUser();
            address = safe(u.getAddress());
            city = safe(u.getCity());
            state = safe(u.getState());
            pin = safe(u.getPinCode());
        }

        String skipped = skippedNotes.isEmpty()
                ? "None"
                : String.join("\n- ", skippedNotes);

        return """
                You are an expert document verification specialist for an Indian green insurance platform. I am sending you %d documents uploaded by a household during their carbon footprint declaration. Your job is to read each document carefully and verify the details against what the user declared.

                WHAT THE USER DECLARED:

                ELECTRICITY:
                  Provider: %s
                  Consumer Number: %s
                  Monthly Average kWh (user claimed): %s kWh
                  Number of bills uploaded: %d

                VEHICLES (%d vehicle(s)):
                %s

                COOKING:
                  Fuel Type: %s
                  %s

                SOLAR:
                  Has Solar: %s
                  %s

                HOUSEHOLD ADDRESS:
                  %s, %s, %s - %s

                DOCUMENTS ATTACHED:
                %s

                DOCUMENTS SKIPPED OR UNREADABLE:
                - %s

                VERIFICATION INSTRUCTIONS:

                ELECTRICITY BILLS:
                - Read every electricity bill document attached.
                - From each bill extract: provider/DISCOM name, consumer number, billing month (MM/YYYY), units consumed (kWh), and amount due.
                - Compare the provider name on bills with what user declared. Allow abbreviations (BESCOM = Bangalore Electricity Supply Company).
                - Check if the same consumer number appears on ALL bills. Flag if any bill has a different consumer number or if consumer number differs from what user declared.
                - Compute the average monthly kWh across all bills.
                - Check if 12 different months are covered.
                - Identify any duplicate months.

                VEHICLE DOCUMENTS:
                - Read each vehicle document.
                - Extract the registration number from the RC book.
                - Extract the fuel type from the RC book.
                - Compare with user declared values.
                - Normalise formats: MH-12-AB-1234 and MH12AB1234 are the same.
                - Fuel type: PETROL = Petrol = P.

                COOKING DOCUMENTS:
                - If LPG: count the number of booking receipts. Extract booking dates and cylinder quantities. Compare count with user declared cylinder count. One receipt = one cylinder booking.
                - If PNG or other: read monthly gas bills. Extract consumer number, month, units consumed. Check 12 months coverage. Check consumer number consistency.

                SOLAR CERTIFICATE:
                - Extract installed capacity in kW.
                - Extract the installation address.
                - Compare capacity with user declared value. Allow +-0.5 kW tolerance.
                - Compare address with household address. Flag if completely different location.

                RESPONSE FORMAT:
                Return ONLY valid JSON matching this exact structure.
                No text before or after. No markdown. Only JSON.

                {
                  "electricity": {
                    "providerMatch": boolean,
                    "providerOnBills": "string or null",
                    "consumerNumberMatch": boolean,
                    "consumerNumberOnBills": "string or null",
                    "consumerNumberConsistent": boolean,
                    "aiComputedMonthlyAvgKwh": number or null,
                    "userDeclaredMonthlyKwh": number,
                    "kwhDifference": number or null,
                    "kwhMatch": boolean,
                    "billsCovered": number,
                    "duplicateMonths": [],
                    "missingMonths": [],
                    "findings": ["specific finding strings"]
                  },
                  "vehicles": [
                    {
                      "vehicleIndex": 0,
                      "registrationNumberOnDoc": "string or null",
                      "registrationNumberMatch": boolean,
                      "fuelTypeOnDoc": "string or null",
                      "fuelTypeMatch": boolean,
                      "documentsRead": number,
                      "findings": []
                    }
                  ],
                  "cooking": {
                    "fuelType": "LPG or PNG etc",
                    "receiptsFound": number,
                    "declaredCylinders": number or null,
                    "cylinderCountMatch": boolean,
                    "distributorName": "string or null",
                    "consumerNumberConsistent": boolean,
                    "findings": []
                  },
                  "solar": {
                    "capacityOnCertificate": number or null,
                    "declaredCapacity": number or null,
                    "capacityMatch": boolean,
                    "addressOnCertificate": "string or null",
                    "addressMatch": boolean,
                    "findings": []
                  },
                  "overallFindings": [
                    {
                      "priority": "HIGH or MEDIUM or LOW",
                      "category": "ELECTRICITY or VEHICLE or COOKING or SOLAR or GENERAL",
                      "finding": "specific finding description",
                      "action": "specific action for agent on site"
                    }
                  ]
                }

                For overallFindings:
                - Only include items that need agent attention.
                - Maximum 8 items. Order by priority HIGH first.
                - Be specific: include actual numbers and names.
                - If everything looks correct, return empty array.
                - Actions must be concrete things the agent can do during a physical household visit.
                """
                .formatted(
                        documentParts.size(),
                        electricityProvider,
                        electricityConsumer,
                        electricityKwh,
                        billCount,
                        vehicles != null ? vehicles.size() : 0,
                        buildVehicleSummary(vehicles),
                        cookingFuelType,
                        buildCookingSummary(cookingData),
                        solarFlag,
                        buildSolarSummary(solarData),
                        address,
                        city,
                        state,
                        pin,
                        buildDocumentLabels(documentParts),
                        skipped);
    }

    private AiDocumentAnalysisResult tryParseFullResult(ObjectMapper mapper, String responseText, Long declarationId) {
        List<String> candidates = expandJsonCandidates(responseText, mapper);
        String lastError = null;
        for (String c : candidates) {
            try {
                JsonNode tree = mapper.readTree(c);
                tree = unwrapJsonNode(tree);
                if (!tree.isObject()) {
                    continue;
                }
                return mapper.treeToValue(tree, AiDocumentAnalysisResult.class);
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.debug("Unable to parse full AI result for declaration {}: {}", declarationId, e.getMessage());
            }
        }
        if (lastError != null) {
            log.debug("Full JSON parse failed for declaration {}: {}", declarationId, lastError);
        }
        return null;
    }

    private AiDocumentAnalysisResult tryParsePartialResult(ObjectMapper mapper, String responseText,
            Long declarationId) {
        List<String> candidates = expandJsonCandidates(responseText, mapper);
        for (String c : candidates) {
            try {
                JsonNode tree = mapper.readTree(c);
                tree = unwrapJsonNode(tree);
                if (!tree.isObject()) {
                    continue;
                }
                Map<String, Object> root = mapper.convertValue(tree, new TypeReference<>() {
                });

                AiDocumentAnalysisResult result = AiDocumentAnalysisResult.builder()
                        .vehicles(new ArrayList<>())
                        .overallFindings(new ArrayList<>())
                        .build();

                result.setElectricity(convertField(mapper, root.get("electricity"), AiElectricityAnalysis.class));
                result.setCooking(convertField(mapper, root.get("cooking"), AiCookingAnalysis.class));
                result.setSolar(convertField(mapper, root.get("solar"), AiSolarAnalysis.class));

                Object vehiclesObj = root.get("vehicles");
                if (vehiclesObj != null) {
                    result.setVehicles(mapper.convertValue(vehiclesObj, new TypeReference<>() {
                    }));
                }

                List<AgentChecklistItem> checklist = extractOverallFindingsFromRoot(mapper, root);
                result.setOverallFindings(checklist);

                boolean hasPayload = result.getElectricity() != null
                        || result.getCooking() != null
                        || result.getSolar() != null
                        || (result.getVehicles() != null && !result.getVehicles().isEmpty())
                        || (result.getOverallFindings() != null && !result.getOverallFindings().isEmpty());

                if (hasPayload) {
                    return result;
                }
            } catch (Exception e) {
                log.debug("Unable to parse partial AI result for declaration {}: {}", declarationId, e.getMessage());
            }
        }
        return null;
    }

    private List<AgentChecklistItem> extractOverallFindingsOnly(ObjectMapper mapper, String responseText,
            Long declarationId) {
        try {
            for (String candidate : expandJsonCandidates(responseText, mapper)) {
                try {
                    JsonNode tree = mapper.readTree(candidate);
                    tree = unwrapJsonNode(tree);
                    if (!tree.isObject()) {
                        continue;
                    }
                    Map<String, Object> root = mapper.convertValue(tree, new TypeReference<>() {
                    });
                    return extractOverallFindingsFromRoot(mapper, root);
                } catch (Exception ignored) {
                    // Try next candidate.
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.debug("Unable to parse overallFindings fallback for declaration {}: {}", declarationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private static <T> T convertField(ObjectMapper mapper, Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.convertValue(value, targetType);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<AgentChecklistItem> extractOverallFindingsFromRoot(ObjectMapper mapper, Map<String, Object> root) {
        Object overallObj = root.get("overallFindings");
        if (overallObj == null) {
            overallObj = root.get("overall_findings");
        }
        if (!(overallObj instanceof List<?> list)) {
            return new ArrayList<>();
        }

        try {
            return mapper.convertValue(list, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            List<AgentChecklistItem> items = new ArrayList<>();
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }
                items.add(new AgentChecklistItem(
                        valueAsString(m.get("priority")),
                        valueAsString(m.get("category")),
                        valueAsString(m.get("finding")),
                        valueAsString(m.get("action"))));
            }
            return items;
        }
    }

    private AiDocumentAnalysisResult enrichWithPerDocumentOcr(
            AiDocumentAnalysisResult result,
            List<DeclarationVehicleData> vehicles,
            ElectricityData electricityData,
            List<ElectricityBill> electricityBills,
            CookingData cookingData) {
        try {
            enrichElectricityFromOcr(result, electricityData, electricityBills);
            enrichVehiclesFromOcr(result, vehicles);
            enrichCookingFromOcr(result, cookingData);
        } catch (Exception e) {
            log.debug("OCR enrichment skipped due to error: {}", e.getMessage());
        }
        return result;
    }

    private void enrichElectricityFromOcr(
            AiDocumentAnalysisResult result,
            ElectricityData electricityData,
            List<ElectricityBill> electricityBills) {
        if (electricityData == null || electricityBills == null || electricityBills.isEmpty()) {
            return;
        }

        AiElectricityAnalysis electricity = result.getElectricity();
        if (electricity == null) {
            electricity = AiElectricityAnalysis.builder()
                    .findings(new ArrayList<>())
                    .duplicateMonths(new ArrayList<>())
                    .missingMonths(new ArrayList<>())
                    .build();
            result.setElectricity(electricity);
        }
        if (electricity.getFindings() == null) {
            electricity.setFindings(new ArrayList<>());
        }

        List<Double> units = new ArrayList<>();
        List<String> providers = new ArrayList<>();
        List<String> consumers = new ArrayList<>();
        int successfulBillExtractions = 0;

        for (ElectricityBill bill : electricityBills) {
            if (bill == null || bill.getBillUrl() == null || bill.getBillUrl().isBlank()) {
                continue;
            }
            DocumentExtractionResult extracted = vertexAiService.extractFromDocument(bill.getBillUrl(),
                    "ELECTRICITY_BILL");
            if (extracted == null || !extracted.isSuccess()) {
                continue;
            }
            successfulBillExtractions++;
            if (extracted.getUnitsKwh() != null) {
                units.add(extracted.getUnitsKwh());
            }
            if (notBlank(extracted.getProviderName())) {
                providers.add(extracted.getProviderName().trim());
            }
            if (notBlank(extracted.getConsumerNumber())) {
                consumers.add(extracted.getConsumerNumber().trim());
            }
        }

        if (!providers.isEmpty() && !notBlank(electricity.getProviderOnBills())) {
            electricity.setProviderOnBills(mostFrequent(providers));
        }
        if (!consumers.isEmpty() && !notBlank(electricity.getConsumerNumberOnBills())) {
            electricity.setConsumerNumberOnBills(mostFrequent(consumers));
        }
        if (!units.isEmpty() && electricity.getAiComputedMonthlyAvgKwh() == null) {
            double avg = units.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            electricity.setAiComputedMonthlyAvgKwh(round2(avg));
        }
        if (electricity.getBillsCovered() <= 0) {
            electricity.setBillsCovered(successfulBillExtractions);
        }

        String declaredProvider = electricityData.getProvider();
        String extractedProvider = electricity.getProviderOnBills();
        if (notBlank(declaredProvider) && notBlank(extractedProvider)) {
            electricity.setProviderMatch(similarText(declaredProvider, extractedProvider));
        }

        String declaredConsumer = electricityData.getConsumerNumber();
        if (notBlank(declaredConsumer) && notBlank(electricity.getConsumerNumberOnBills())) {
            String declaredNorm = normalizeIdentifier(declaredConsumer);
            String extractedNorm = normalizeIdentifier(electricity.getConsumerNumberOnBills());
            electricity.setConsumerNumberMatch(declaredNorm.equals(extractedNorm));
        }
        if (!consumers.isEmpty()) {
            String first = normalizeIdentifier(consumers.get(0));
            boolean consistent = true;
            for (String c : consumers) {
                if (!first.equals(normalizeIdentifier(c))) {
                    consistent = false;
                    break;
                }
            }
            electricity.setConsumerNumberConsistent(consistent);
        }

        Double userDeclared = electricityData.getUserDeclaredMonthlyKwh();
        if (userDeclared != null) {
            electricity.setUserDeclaredMonthlyKwh(userDeclared);
        }
        if (electricity.getAiComputedMonthlyAvgKwh() != null && electricity.getUserDeclaredMonthlyKwh() != null) {
            double diff = Math.abs(electricity.getAiComputedMonthlyAvgKwh() - electricity.getUserDeclaredMonthlyKwh());
            electricity.setKwhDifference(round2(diff));
            electricity.setKwhMatch(diff <= Math.max(15.0, electricity.getUserDeclaredMonthlyKwh() * 0.2));
        }

        if (!units.isEmpty()
                && electricity.getFindings().stream().noneMatch(s -> s != null && s.contains("OCR cross-check"))) {
            electricity.getFindings().add("OCR cross-check extracted values from " + units.size() + " bill(s).");
        }
        if (successfulBillExtractions > 0 && (!providers.isEmpty() || !consumers.isEmpty() || !units.isEmpty())) {
            removeGenericFinding(electricity.getFindings(), "could not be extracted reliably");
        }
    }

    private void enrichVehiclesFromOcr(AiDocumentAnalysisResult result, List<DeclarationVehicleData> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }

        if (result.getVehicles() == null) {
            result.setVehicles(new ArrayList<>());
        }

        while (result.getVehicles().size() < vehicles.size()) {
            result.getVehicles().add(AiVehicleAnalysis.builder()
                    .vehicleIndex(result.getVehicles().size())
                    .findings(new ArrayList<>())
                    .build());
        }

        for (int i = 0; i < vehicles.size(); i++) {
            DeclarationVehicleData declared = vehicles.get(i);
            AiVehicleAnalysis aiVehicle = result.getVehicles().get(i);
            if (aiVehicle.getFindings() == null) {
                aiVehicle.setFindings(new ArrayList<>());
            }

            String extractedReg = null;
            String extractedFuel = null;
            int docsRead = 0;

            if (declared.getDocuments() != null) {
                for (VehicleDocument doc : declared.getDocuments()) {
                    if (doc == null || doc.getDocumentUrl() == null || doc.getDocumentUrl().isBlank()) {
                        continue;
                    }
                    String docType = doc.getDocumentType() != null ? doc.getDocumentType().name() : "OTHER";
                    DocumentExtractionResult extracted = vertexAiService.extractFromDocument(doc.getDocumentUrl(),
                            docType);
                    if (extracted == null || !extracted.isSuccess()) {
                        continue;
                    }
                    docsRead++;
                    if (!notBlank(extractedReg) && notBlank(extracted.getRegistrationNumber())) {
                        extractedReg = extracted.getRegistrationNumber().trim();
                    }
                    if (!notBlank(extractedFuel) && notBlank(extracted.getFuelType())) {
                        extractedFuel = extracted.getFuelType().trim();
                    }
                }
            }

            if (!notBlank(aiVehicle.getRegistrationNumberOnDoc()) && notBlank(extractedReg)) {
                aiVehicle.setRegistrationNumberOnDoc(extractedReg);
            }
            if (!notBlank(aiVehicle.getFuelTypeOnDoc()) && notBlank(extractedFuel)) {
                aiVehicle.setFuelTypeOnDoc(extractedFuel);
            }
            if (aiVehicle.getDocumentsRead() <= 0) {
                aiVehicle.setDocumentsRead(docsRead);
            }

            if (notBlank(declared.getRegistrationNumber()) && notBlank(aiVehicle.getRegistrationNumberOnDoc())) {
                aiVehicle.setRegistrationNumberMatch(
                        normalizeRegistration(declared.getRegistrationNumber())
                                .equals(normalizeRegistration(aiVehicle.getRegistrationNumberOnDoc())));
            }
            if (declared.getFuelType() != null && notBlank(aiVehicle.getFuelTypeOnDoc())) {
                aiVehicle.setFuelTypeMatch(
                        normalizeFuel(declared.getFuelType().name())
                                .equals(normalizeFuel(aiVehicle.getFuelTypeOnDoc())));
            }
            if (notBlank(aiVehicle.getRegistrationNumberOnDoc()) || notBlank(aiVehicle.getFuelTypeOnDoc())) {
                removeGenericFinding(aiVehicle.getFindings(), "Unable to extract reliable vehicle values");
            }
        }
    }

    private void enrichCookingFromOcr(AiDocumentAnalysisResult result, CookingData cookingData) {
        if (cookingData == null || cookingData.getBillUrls() == null || cookingData.getBillUrls().isBlank()) {
            return;
        }

        AiCookingAnalysis cooking = result.getCooking();
        if (cooking == null) {
            cooking = AiCookingAnalysis.builder().findings(new ArrayList<>()).build();
            result.setCooking(cooking);
        }
        if (cooking.getFindings() == null) {
            cooking.setFindings(new ArrayList<>());
        }

        List<String> urls = parseUrlList(cookingData.getBillUrls());
        String docType = cookingData.getFuelType() != null && "PNG".equalsIgnoreCase(cookingData.getFuelType().name())
                ? "PNG_BILL"
                : "LPG_RECEIPT";

        int successCount = 0;
        int cylinderTotal = 0;
        List<String> consumers = new ArrayList<>();
        String distributor = null;

        for (String url : urls) {
            DocumentExtractionResult extracted = vertexAiService.extractFromDocument(url, docType);
            if (extracted == null || !extracted.isSuccess()) {
                continue;
            }
            successCount++;
            if (extracted.getCylinderCount() != null) {
                cylinderTotal += extracted.getCylinderCount();
            }
            if (!notBlank(distributor) && notBlank(extracted.getDistributorName())) {
                distributor = extracted.getDistributorName().trim();
            }
            if (notBlank(extracted.getConsumerNumber())) {
                consumers.add(extracted.getConsumerNumber().trim());
            }
        }

        if (cooking.getReceiptsFound() <= 0) {
            cooking.setReceiptsFound(successCount);
        }
        if (cookingData.getUserDeclaredCylinders() != null) {
            cooking.setDeclaredCylinders(cookingData.getUserDeclaredCylinders());
        }
        if (notBlank(distributor) && !notBlank(cooking.getDistributorName())) {
            cooking.setDistributorName(distributor);
        }
        if (!consumers.isEmpty()) {
            String first = normalizeIdentifier(consumers.get(0));
            boolean consistent = true;
            for (String c : consumers) {
                if (!first.equals(normalizeIdentifier(c))) {
                    consistent = false;
                    break;
                }
            }
            cooking.setConsumerNumberConsistent(consistent);
        }

        if ("LPG".equalsIgnoreCase(cookingData.getFuelType() != null ? cookingData.getFuelType().name() : "")) {
            Integer declared = cookingData.getUserDeclaredCylinders();
            if (declared != null && successCount > 0) {
                int effectiveCount = cylinderTotal > 0 ? cylinderTotal : successCount;
                cooking.setCylinderCountMatch(declared == effectiveCount);
            }
        }
        if (successCount > 0 || notBlank(distributor)) {
            removeGenericFinding(cooking.getFindings(), "documents were unclear or unstructured");
        }
    }

    private static void removeGenericFinding(List<String> findings, String fragment) {
        if (findings == null || findings.isEmpty() || fragment == null || fragment.isBlank()) {
            return;
        }
        findings.removeIf(f -> f != null && f.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT)));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String mostFrequent(List<String> values) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String value : values) {
            if (!notBlank(value)) {
                continue;
            }
            String key = value.trim();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        String winner = null;
        int winnerCount = -1;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > winnerCount) {
                winner = entry.getKey();
                winnerCount = entry.getValue();
            }
        }
        return winner;
    }

    private static String normalizeIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeRegistration(String value) {
        return normalizeIdentifier(value);
    }

    private static String normalizeFuel(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if ("P".equals(normalized)) {
            return "PETROL";
        }
        if ("D".equals(normalized)) {
            return "DIESEL";
        }
        return normalized;
    }

    private static boolean similarText(String left, String right) {
        String l = normalizeIdentifier(left);
        String r = normalizeIdentifier(right);
        return !l.isEmpty() && !r.isEmpty() && (l.equals(r) || l.contains(r) || r.contains(l));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    /**
     * Builds strings to try as JSON: BOM-stripped text, balanced object slice,
     * normalized quotes, array elements, etc.
     */
    private List<String> expandJsonCandidates(String responseText, ObjectMapper mapper) {
        List<String> candidates = new ArrayList<>();
        if (responseText == null) {
            return candidates;
        }
        String text = stripBom(responseText).trim();
        if (text.isEmpty()) {
            return candidates;
        }

        addCandidate(candidates, text);
        String objectSlice = extractBalancedJsonObject(text);
        if (objectSlice != null && !objectSlice.equals(text)) {
            addCandidate(candidates, objectSlice);
        }

        if (text.startsWith("[")) {
            try {
                JsonNode arr = mapper.readTree(text);
                if (arr.isArray()) {
                    for (JsonNode el : arr) {
                        if (el.isObject()) {
                            addCandidate(candidates, mapper.writeValueAsString(el));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not parse JSON array root: {}", e.getMessage());
            }
        }
        return candidates;
    }

    private static String stripBom(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    /**
     * Unwraps {@code [{...}]} or {@code {"result":{...}}} style trees until we
     * reach a stable object or non-wrapper.
     */
    private static JsonNode unwrapJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isArray() && node.size() == 1) {
            return unwrapJsonNode(node.get(0));
        }
        if (node.isObject() && node.size() == 1) {
            String field = node.fieldNames().next();
            if (field != null && JSON_WRAPPER_KEYS.contains(field.toLowerCase(Locale.ROOT))) {
                return unwrapJsonNode(node.get(field));
            }
        }
        return node;
    }

    private void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }

        String trimmed = candidate.trim();
        if (!trimmed.isBlank()) {
            candidates.add(trimmed);
        }

        String normalized = normalizeJsonCandidate(trimmed);
        if (!normalized.equals(trimmed)) {
            candidates.add(normalized);
        }
    }

    private String normalizeJsonCandidate(String input) {
        String normalized = input
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'');

        // Remove trailing commas before object/array close to salvage near-valid JSON.
        normalized = TRAILING_COMMA_PATTERN.matcher(normalized).replaceAll("$1");
        return normalized;
    }

    private AiDocumentAnalysisResult failedResult(String message, LocalDateTime analysedAt) {
        return AiDocumentAnalysisResult.builder()
                .analysisSuccess(false)
                .errorMessage(message)
                .overallFindings(new ArrayList<>())
                .analysedAt(analysedAt)
                .build();
    }

    private AiDocumentAnalysisResult buildGracefulManualFallbackResult(
            List<AgentChecklistItem> fallbackFindings,
            List<String> skippedNotes,
            List<DeclarationVehicleData> vehicles,
            ElectricityData electricityData,
            CookingData cookingData,
            SolarData solarData,
            LocalDateTime analysedAt) {

        List<AgentChecklistItem> findings = new ArrayList<>();
        if (fallbackFindings != null) {
            findings.addAll(fallbackFindings);
        }

        List<AiVehicleAnalysis> vehicleFallback = new ArrayList<>();
        if (vehicles != null) {
            for (int i = 0; i < vehicles.size(); i++) {
                DeclarationVehicleData v = vehicles.get(i);
                int docCount = v.getDocuments() != null ? v.getDocuments().size() : 0;
                vehicleFallback.add(AiVehicleAnalysis.builder()
                        .vehicleIndex(i)
                        .registrationNumberOnDoc(null)
                        .registrationNumberMatch(false)
                        .fuelTypeOnDoc(null)
                        .fuelTypeMatch(false)
                        .documentsRead(docCount)
                        .findings(new ArrayList<>(List.of(
                                "Unable to extract reliable vehicle values from uploaded files; verify RC details on site.")))
                        .build());
            }
        }

        AiElectricityAnalysis electricityFallback = null;
        if (electricityData != null) {
            electricityFallback = AiElectricityAnalysis.builder()
                    .providerMatch(false)
                    .providerOnBills(null)
                    .consumerNumberMatch(false)
                    .consumerNumberOnBills(null)
                    .consumerNumberConsistent(false)
                    .aiComputedMonthlyAvgKwh(null)
                    .userDeclaredMonthlyKwh(electricityData.getUserDeclaredMonthlyKwh())
                    .kwhDifference(null)
                    .kwhMatch(false)
                    .billsCovered(0)
                    .findings(new ArrayList<>(List.of(
                            "Electricity bill data could not be extracted reliably; compare provider, consumer number, and units manually.")))
                    .build();
        }

        AiCookingAnalysis cookingFallback = null;
        if (cookingData != null) {
            cookingFallback = AiCookingAnalysis.builder()
                    .fuelType(cookingData.getFuelType() != null ? cookingData.getFuelType().name() : null)
                    .receiptsFound(0)
                    .declaredCylinders(cookingData.getUserDeclaredCylinders())
                    .cylinderCountMatch(false)
                    .distributorName(null)
                    .consumerNumberConsistent(false)
                    .findings(new ArrayList<>(List.of(
                            "Cooking fuel documents were unclear or unstructured; validate receipts/consumer numbers manually.")))
                    .build();
        }

        AiSolarAnalysis solarFallback = null;
        if (solarData != null && solarData.isHasSolar()) {
            solarFallback = AiSolarAnalysis.builder()
                    .capacityOnCertificate(null)
                    .declaredCapacity(solarData.getCapacityKw())
                    .capacityMatch(false)
                    .addressOnCertificate(null)
                    .addressMatch(false)
                    .findings(new ArrayList<>(List.of(
                            "Solar certificate details could not be extracted reliably; verify capacity and address manually.")))
                    .build();
        }

        if (skippedNotes != null && !skippedNotes.isEmpty()) {
            int maxNotes = Math.min(3, skippedNotes.size());
            for (int i = 0; i < maxNotes; i++) {
                findings.add(new AgentChecklistItem(
                        "MEDIUM",
                        "GENERAL",
                        skippedNotes.get(i),
                        "Open the source file during site visit and verify with physical originals."));
            }
        }

        if (findings.isEmpty()) {
            findings.add(new AgentChecklistItem(
                    "MEDIUM",
                    "GENERAL",
                    "Uploaded files were readable but did not provide reliably structured values for extraction.",
                    "Use the comparison table and uploaded documents to validate key identifiers and totals before final decision."));
        }

        return AiDocumentAnalysisResult.builder()
                .analysisSuccess(true)
                .errorMessage(null)
                .electricity(electricityFallback)
                .vehicles(vehicleFallback)
                .cooking(cookingFallback)
                .solar(solarFallback)
                .overallFindings(findings)
                .analysedAt(analysedAt)
                .build();
    }

    private String buildVehicleSummary(List<DeclarationVehicleData> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return "No vehicle data provided";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vehicles.size(); i++) {
            DeclarationVehicleData v = vehicles.get(i);
            sb.append("Vehicle ")
                    .append(i + 1)
                    .append(": ")
                    .append(safe(v.getMake()))
                    .append(" ")
                    .append(safe(v.getModel()))
                    .append(" ")
                    .append(v.getManufacturingYear() != null ? v.getManufacturingYear() : "")
                    .append(", Reg: ")
                    .append(safe(v.getRegistrationNumber()))
                    .append(", Fuel: ")
                    .append(v.getFuelType() != null ? v.getFuelType().name() : "Not provided")
                    .append(", Category: ")
                    .append(v.getVehicleCategory() != null ? v.getVehicleCategory().name() : "Not provided")
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildCookingSummary(CookingData c) {
        if (c == null) {
            return "No cooking data provided";
        }
        if (c.getFuelType() != null && "LPG".equals(c.getFuelType().name())) {
            return "Cylinders per year declared: "
                    + (c.getUserDeclaredCylinders() != null ? c.getUserDeclaredCylinders() : "Not provided");
        }
        return "Consumer Number: " + safe(c.getPngConsumerNumber());
    }

    private String buildSolarSummary(SolarData s) {
        if (s == null || !s.isHasSolar()) {
            return "No solar installation";
        }
        return "Capacity: " + (s.getCapacityKw() != null ? s.getCapacityKw() : "Not provided")
                + " kW, Certificate URL: " + safe(s.getCertificateUrl());
    }

    private String buildDocumentLabels(List<DocumentPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append("Document ").append(i + 1).append(": ").append(parts.get(i).label()).append("\n");
        }
        return sb.toString();
    }

    private byte[] readFileFromUrl(String fileUrl) throws IOException {
        String relativePath = fileUrl.trim();
        if (relativePath.startsWith("http://localhost:9090/")) {
            relativePath = relativePath.replaceFirst("http://localhost:9090/", "");
        } else if (relativePath.startsWith("http://127.0.0.1:9090/")) {
            relativePath = relativePath.replaceFirst("http://127.0.0.1:9090/", "");
        } else if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        if (relativePath.startsWith("uploads/")) {
            relativePath = relativePath.substring("uploads/".length());
        }

        Path diskPath = Paths.get(uploadDir).resolve(relativePath).normalize();
        return Files.readAllBytes(diskPath);
    }

    private String detectMimeType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    private String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String buildVertexUrl(String model) {
        return String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                region,
                projectId,
                region,
                model);
    }

    private String attemptJsonRepairWithModel(String rawOutput, HttpHeaders headers, Long declarationId) {
        try {
            String repairPrompt = buildJsonRepairPrompt(rawOutput);
            Map<String, Object> body = buildTextOnlyRequestBody(repairPrompt);
            ResponseEntity<Map> repairResponse = restTemplate.postForEntity(
                    buildVertexUrl(vertexModelId),
                    new HttpEntity<>(body, headers),
                    Map.class);
            String repairedText = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(repairResponse.getBody());
            if (repairedText == null || repairedText.isBlank()) {
                return null;
            }
            return repairedText;
        } catch (Exception e) {
            log.debug("AI JSON repair pass failed for declaration {}: {}", declarationId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildTextOnlyRequestBody(String prompt) {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", vertexJsonGenerationConfig(0.0, 3000));
        return requestBody;
    }

    /**
     * Minimal {@code generationConfig} for Gemini on Vertex (avoids unsupported
     * combo fields on 2.x Flash).
     */
    private static Map<String, Object> vertexJsonGenerationConfig(double temperature, int maxOutputTokens) {
        return vertexJsonGenerationConfig(temperature, maxOutputTokens, false);
    }

    private static Map<String, Object> vertexJsonGenerationConfig(
            double temperature,
            int maxOutputTokens,
            boolean includeSchema) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", "application/json");
        if (includeSchema) {
            generationConfig.put("responseSchema", buildAnalysisResponseSchema());
        }
        return generationConfig;
    }

    private static Map<String, Object> buildAnalysisResponseSchema() {
        Map<String, Object> stringOrNull = new LinkedHashMap<>();
        stringOrNull.put("type", "STRING");
        stringOrNull.put("nullable", true);

        Map<String, Object> numberOrNull = new LinkedHashMap<>();
        numberOrNull.put("type", "NUMBER");
        numberOrNull.put("nullable", true);

        Map<String, Object> integerOrNull = new LinkedHashMap<>();
        integerOrNull.put("type", "INTEGER");
        integerOrNull.put("nullable", true);

        Map<String, Object> findingsArray = new LinkedHashMap<>();
        findingsArray.put("type", "ARRAY");
        findingsArray.put("items", Map.of("type", "STRING"));

        Map<String, Object> electricityProps = new LinkedHashMap<>();
        electricityProps.put("providerMatch", Map.of("type", "BOOLEAN"));
        electricityProps.put("providerOnBills", stringOrNull);
        electricityProps.put("consumerNumberMatch", Map.of("type", "BOOLEAN"));
        electricityProps.put("consumerNumberOnBills", stringOrNull);
        electricityProps.put("consumerNumberConsistent", Map.of("type", "BOOLEAN"));
        electricityProps.put("aiComputedMonthlyAvgKwh", numberOrNull);
        electricityProps.put("userDeclaredMonthlyKwh", numberOrNull);
        electricityProps.put("kwhDifference", numberOrNull);
        electricityProps.put("kwhMatch", Map.of("type", "BOOLEAN"));
        electricityProps.put("billsCovered", Map.of("type", "INTEGER"));
        electricityProps.put("duplicateMonths", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        electricityProps.put("missingMonths", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        electricityProps.put("findings", findingsArray);

        Map<String, Object> electricity = new LinkedHashMap<>();
        electricity.put("type", "OBJECT");
        electricity.put("properties", electricityProps);

        Map<String, Object> vehicleProps = new LinkedHashMap<>();
        vehicleProps.put("vehicleIndex", Map.of("type", "INTEGER"));
        vehicleProps.put("registrationNumberOnDoc", stringOrNull);
        vehicleProps.put("registrationNumberMatch", Map.of("type", "BOOLEAN"));
        vehicleProps.put("fuelTypeOnDoc", stringOrNull);
        vehicleProps.put("fuelTypeMatch", Map.of("type", "BOOLEAN"));
        vehicleProps.put("documentsRead", Map.of("type", "INTEGER"));
        vehicleProps.put("findings", findingsArray);

        Map<String, Object> vehicles = new LinkedHashMap<>();
        vehicles.put("type", "ARRAY");
        vehicles.put("items", Map.of("type", "OBJECT", "properties", vehicleProps));

        Map<String, Object> cookingProps = new LinkedHashMap<>();
        cookingProps.put("fuelType", stringOrNull);
        cookingProps.put("receiptsFound", Map.of("type", "INTEGER"));
        cookingProps.put("declaredCylinders", integerOrNull);
        cookingProps.put("cylinderCountMatch", Map.of("type", "BOOLEAN"));
        cookingProps.put("distributorName", stringOrNull);
        cookingProps.put("consumerNumberConsistent", Map.of("type", "BOOLEAN"));
        cookingProps.put("findings", findingsArray);

        Map<String, Object> cooking = new LinkedHashMap<>();
        cooking.put("type", "OBJECT");
        cooking.put("properties", cookingProps);

        Map<String, Object> solarProps = new LinkedHashMap<>();
        solarProps.put("capacityOnCertificate", numberOrNull);
        solarProps.put("declaredCapacity", numberOrNull);
        solarProps.put("capacityMatch", Map.of("type", "BOOLEAN"));
        solarProps.put("addressOnCertificate", stringOrNull);
        solarProps.put("addressMatch", Map.of("type", "BOOLEAN"));
        solarProps.put("findings", findingsArray);

        Map<String, Object> solar = new LinkedHashMap<>();
        solar.put("type", "OBJECT");
        solar.put("properties", solarProps);

        Map<String, Object> checklistItemProps = new LinkedHashMap<>();
        checklistItemProps.put("priority", stringOrNull);
        checklistItemProps.put("category", stringOrNull);
        checklistItemProps.put("finding", stringOrNull);
        checklistItemProps.put("action", stringOrNull);

        Map<String, Object> overallFindings = new LinkedHashMap<>();
        overallFindings.put("type", "ARRAY");
        overallFindings.put("items", Map.of("type", "OBJECT", "properties", checklistItemProps));

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("electricity", electricity);
        rootProps.put("vehicles", vehicles);
        rootProps.put("cooking", cooking);
        rootProps.put("solar", solar);
        rootProps.put("overallFindings", overallFindings);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", rootProps);
        return schema;
    }

    private String buildJsonRepairPrompt(String rawOutput) {
        String clipped = rawOutput == null ? "" : rawOutput;
        int maxChars = 12000;
        if (clipped.length() > maxChars) {
            clipped = clipped.substring(0, maxChars);
        }

        return """
                Convert the following model output into strict valid JSON only.
                Rules:
                - Return a single JSON object only.
                - No markdown, no code fences, no explanations.
                - Preserve values from source as much as possible.
                - If a field is missing, use null, 0, false, or [] as appropriate.
                - Ensure top-level keys are exactly: electricity, vehicles, cooking, solar, overallFindings.

                Source output:
                %s
                """.formatted(clipped);
    }

    private ResponseEntity<Map> postForEntityWithRetry(
            String url,
            HttpEntity<Map<String, Object>> entity,
            Long declarationId) {

        int totalAttempts = Math.max(1, aiRequestRetries + 1);
        RestClientException last = null;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                return restTemplate.postForEntity(url, entity, Map.class);
            } catch (RestClientException ex) {
                last = ex;
                boolean retryableTimeout = isRetryableTimeout(ex);
                if (!retryableTimeout || attempt == totalAttempts) {
                    throw ex;
                }

                long delayMs = Math.max(0L, aiRequestRetryBackoffMs) * attempt;
                log.warn(
                        "AI analysis timeout for declaration {} (attempt {}/{}). Retrying in {} ms.",
                        declarationId,
                        attempt,
                        totalAttempts,
                        delayMs);
                sleepQuietly(delayMs);
            }
        }

        throw last;
    }

    private static boolean isRetryableTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepQuietly(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("AI retry wait interrupted");
        }
    }

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

    private List<String> parseUrlList(String rawJsonArray) {
        try {
            return objectMapper.readValue(rawJsonArray, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            String cleaned = rawJsonArray.replace("[", "").replace("]", "").replace("\"", "");
            List<String> values = new ArrayList<>();
            for (String part : cleaned.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    values.add(t);
                }
            }
            return values;
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private static String valueAsString(Object value) {
        return value == null ? "" : value.toString();
    }
}
