package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.enums.MatchStatus;

import java.util.List;

/**
 * The agent verification workspace payload.
 * Contains user claims, system-verified values, a comparison table,
 * and the fraud advisory result.
 *
 * The comparison table rows use the ComparisonField record
 * so the frontend can render a structured table automatically.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentWorkspaceResponse {

    private Long   declarationId;
    private Long   assignmentId;
    private String userName;
    private String userAddress;
    private String userPhone;
    private String pinCode;
    private Integer householdMemberCount;
    private Integer declarationYear;

    // ── Vehicle Multiple Comparisons ───────────────────────────
    @lombok.Builder.Default
    private List<VehicleComparisonBlock> vehicles = new java.util.ArrayList<>();

    private Double  userDeclaredMonthlyKwh;
    private Double  ocrComputedMonthlyKwh;
    private Integer billsUploaded;
    @lombok.Builder.Default
    private List<ComparisonField> electricityComparison = new java.util.ArrayList<>();
    @lombok.Builder.Default
    private List<String> electricityDocumentUrls = new java.util.ArrayList<>();

    private String  cookingFuelType;
    private Integer userDeclaredCylinders;
    private Integer ocrComputedCylinders;
    @lombok.Builder.Default
    private List<ComparisonField> cookingComparison = new java.util.ArrayList<>();
    @lombok.Builder.Default
    private List<String> cookingDocumentUrls = new java.util.ArrayList<>();

    private Boolean hasSolar;
    private Double  solarCapacityKw;
    private Boolean mnreVerified;
    private String  solarCertificateUrl;
    @lombok.Builder.Default
    private List<ComparisonField> solarComparison = new java.util.ArrayList<>();
    @lombok.Builder.Default
    private List<String> solarDocumentUrls = new java.util.ArrayList<>();

    // ── Fraud Advisory ─────────────────────────────────────────
    private String fraudRiskLevel;
    private List<String> fraudFlags;
    /** Parallel human-readable descriptions for {@link #fraudFlags}. */
    @lombok.Builder.Default
    private List<String> fraudFlagDescriptions = new java.util.ArrayList<>();
    private Integer fraudScore;

    /**
     * Vertex AI checklist for the site visit (structured).
     */
    @lombok.Builder.Default
    private List<AgentChecklistItem> aiVerificationChecklist = new java.util.ArrayList<>();

    // ── Comparison Table ───────────────────────────────────────

    /** One row per data field being compared. */
    @lombok.Builder.Default
    private List<ComparisonField> comparisonTable = new java.util.ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ComparisonField {
        private String      fieldName;
        private String      userClaim;
        private String      systemValue;
        private MatchStatus matchStatus;
        private String      note;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VehicleComparisonBlock {
        private String vehicleLabel;
        private String vehicleCategory;
        @lombok.Builder.Default
        private List<ComparisonField> comparisons = new java.util.ArrayList<>();
        @lombok.Builder.Default
        private List<org.hartford.greensure.dto.response.VehicleDocumentResponseDTO> documents = new java.util.ArrayList<>();
    }
}
