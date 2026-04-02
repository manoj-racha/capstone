package org.hartford.greensure.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDocumentAnalysisResult {

    private AiElectricityAnalysis electricity;

    @Builder.Default
    private List<AiVehicleAnalysis> vehicles = new ArrayList<>();

    private AiCookingAnalysis cooking;
    private AiSolarAnalysis solar;

    @JsonAlias("overall_findings")
    @Builder.Default
    private List<AgentChecklistItem> overallFindings = new ArrayList<>();

    private boolean analysisSuccess;
    private String errorMessage;
    private LocalDateTime analysedAt;
}
