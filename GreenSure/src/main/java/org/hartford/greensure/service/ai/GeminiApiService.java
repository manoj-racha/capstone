package org.hartford.greensure.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.entity.CarbonScore;
import org.hartford.greensure.entity.CookingData;
import org.hartford.greensure.entity.DeclarationVehicleData;
import org.hartford.greensure.entity.HouseholdProfile;
import org.hartford.greensure.entity.SolarData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Calls Google Gemini (Google AI Studio) for short, factual text generation.
 * Used for the carbon score explainer; failures always degrade to a template
 * string.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiApiService {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${ai.score.explainer.enabled:true}")
    private boolean scoreExplainerEnabled;

    @Value("${ai.diagnostic.log.raw.model.response:${ai.diagnostic.log.raw.vertex.response:false}}")
    private boolean logRawModelResponse;

    public String generateScoreExplanation(
            CarbonScore score,
            HouseholdProfile profile,
            List<DeclarationVehicleData> vehicles,
            CookingData cookingData,
            SolarData solarData,
            Double previousYearCo2) {
        try {
            if (!scoreExplainerEnabled) {
                return buildFallbackExplanation(score);
            }
            Objects.requireNonNull(score, "score");

            int members = profile != null && profile.getNumberOfMembers() != null
                    ? profile.getNumberOfMembers()
                    : 1;

            String vehicleSummary = buildVehicleSummary(vehicles);

            String cookingFuel = "not specified";
            if (cookingData != null && cookingData.getEffectiveFuelType() != null) {
                cookingFuel = cookingData.getEffectiveFuelType().name().replace("_", " ");
            }

            boolean hasSolar = solarData != null && solarData.isHasSolar();

            String previousYearBlock = "";
            if (previousYearCo2 != null && previousYearCo2 > 0 && score.getTotalCo2() != null) {
                double change = ((previousYearCo2 - score.getTotalCo2()) / previousYearCo2) * 100.0;
                String direction = change >= 0 ? "improved" : "worsened";
                previousYearBlock = String.format(
                        "- Previous year CO₂: %.0f kg (%.0f%% %s from last year)%n",
                        previousYearCo2,
                        Math.abs(change),
                        direction);
            }

            double urbanRef = 1900.0;
            String prompt = String.format(
                    """
                            You are a carbon footprint advisor for Indian households.

                            Write a clear, friendly explanation in 5-7 sentences, using simple language and no jargon.
                            Keep the output as one readable paragraph (no bullets, no markdown, no headings).

                            Required content:
                            1) Brief overall assessment of their carbon profile.
                            2) Mention their total CO2 and per-capita CO2 with numbers.
                            3) Explain which emission source contributes the most and why that matters.
                            4) Compare their per-capita CO2 with the urban reference.
                            5) Mention their zone and discount in plain terms.
                            6) Give exactly two practical, high-impact next steps tailored to their biggest source.

                            Tone requirements:
                            - encouraging and non-judgmental
                            - specific and actionable
                            - no generic motivational filler

                                Household data:
                                - Members: %d
                                - Total CO₂: %.0f kg/year
                                - Vehicle emissions: %.0f kg (vehicles: %s)
                                - Electricity emissions: %.0f kg
                                - Cooking emissions: %.0f kg (fuel: %s)
                                - Solar offset: %.0f kg (%s)
                                - Lifestyle bonus: %.0f kg saved
                                - Per capita CO₂: %.0f kg/person/year
                                - Zone: %s
                                - Discount earned: %.0f%%
                                %s
                                Indian context for reference:
                                - Urban average: %.0f kg/person/year
                                - GREEN_CHAMPION threshold: below 1,500 kg/person
                                - IMPROVER threshold: 1,500-4,000 kg/person
                                - DEFAULTER: above 4,000 kg/person

                                Write only the explanation paragraph. No headers. No bullets. No markdown.
                                """,
                    members,
                    nz(score.getTotalCo2()),
                    nz(score.getVehicleCo2()),
                    vehicleSummary,
                    nz(score.getElectricityCo2()),
                    nz(score.getCookingCo2()),
                    cookingFuel,
                    nz(score.getSolarOffset()),
                    hasSolar ? "solar installed" : "no solar",
                    nz(score.getLifestyleBonus()),
                    nz(score.getPerCapitaCo2()),
                    score.getZone() != null ? score.getZone().name() : "UNKNOWN",
                    nz(score.getDiscountPercent()),
                    previousYearBlock,
                    urbanRef);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 520);

            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> partsWrapper = new HashMap<>();
            partsWrapper.put("parts", List.of(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(partsWrapper));
            requestBody.put("generationConfig", generationConfig);

            String url = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (logRawModelResponse) {
                log.info(
                        "Gemini (AI Studio) raw response: {}",
                        VertexGeminiResponseUtil.truncateForLog(String.valueOf(response.getBody()), 12000));
            }
            String text = VertexGeminiResponseUtil.extractTextFromGeminiResponseBody(response.getBody());
            if (logRawModelResponse && text != null) {
                log.info(
                        "Gemini extracted model text: {}",
                        VertexGeminiResponseUtil.truncateForLog(text, 12000));
            }
            if (text == null || text.isBlank()) {
                log.warn("Gemini returned empty text; using fallback explanation");
                return buildFallbackExplanation(score);
            }
            return text.trim();
        } catch (RestClientException e) {
            log.error("Gemini API request failed: {}", e.getMessage());
            return buildFallbackExplanation(score);
        } catch (Exception e) {
            log.error("Gemini score explanation failed: {}", e.getMessage(), e);
            return buildFallbackExplanation(score);
        }
    }

    private static String buildVehicleSummary(List<DeclarationVehicleData> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return "none declared";
        }
        return vehicles.stream()
                .map(v -> v.getEffectiveFuelType() != null
                        ? v.getEffectiveFuelType().name().replace("_", " ")
                        : "unknown fuel")
                .collect(Collectors.joining(" + "));
    }

    private static double nz(Double v) {
        return v != null ? v : 0.0;
    }

    private String buildFallbackExplanation(CarbonScore score) {
        double discount = score.getDiscountPercent() != null ? score.getDiscountPercent() : 0.0;
        String zoneLabel = score.getZone() != null
                ? score.getZone().name().replace("_", " ")
                : "unknown";
        double total = nz(score.getTotalCo2());
        double perCapita = nz(score.getPerCapitaCo2());
        String biggest = determineBiggestSource(score);
        double biggestValue = determineBiggestValue(score);
        return String.format(
                "Your household currently emits about %.0f kg of CO₂ per year, with a per-capita footprint"
                        + " of %.0f kg. This places you in the %s zone and corresponds to an insurance"
                        + " discount of %.0f%%. Your largest emission source is %s at %.0f kg, so the"
                        + " strongest improvement will come from reducing that category first."
                        + " Focus on two practical actions this cycle: cut high-consumption usage in your"
                        + " largest source, and track monthly changes so the next declaration shows measurable progress.",
                total,
                perCapita,
                zoneLabel,
                discount,
                biggest,
                biggestValue);
    }

    private String determineBiggestSource(CarbonScore s) {
        double v = nz(s.getVehicleCo2());
        double e = nz(s.getElectricityCo2());
        double c = nz(s.getCookingCo2());
        if (v >= e && v >= c) {
            return "vehicle";
        }
        if (e >= v && e >= c) {
            return "electricity";
        }
        return "cooking";
    }

    private double determineBiggestValue(CarbonScore s) {
        double v = nz(s.getVehicleCo2());
        double e = nz(s.getElectricityCo2());
        double c = nz(s.getCookingCo2());
        return Math.max(v, Math.max(e, c));
    }
}
