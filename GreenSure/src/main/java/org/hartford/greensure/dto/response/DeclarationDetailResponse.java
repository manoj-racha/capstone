package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.enums.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full declaration detail returned when the user (or admin) requests a single declaration.
 * Includes all 7 module data snapshots plus carbon score when VERIFIED.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeclarationDetailResponse {

    private Long              declarationId;
    private Long              userId;
    private String            fullName;
    private Integer           declarationYear;
    private DeclarationStatus status;
    private Integer           resubmissionCount;
    private LocalDateTime     submittedAt;
    private LocalDateTime     createdAt;

    // Module 2
    private Integer householdMembers;

    // Module 3
    private List<VehicleResponseDTO> vehicles;

    // Module 4
    private String provider;
    private String consumerNumber;
    private Double userDeclaredMonthlyKwh;
    private Double ocrComputedMonthlyKwh;
    private Integer billsUploaded;
    private List<String> electricityBillUrls;

    /** Per-bill AI/OCR confidence and anomaly summary for the review screen. */
    @Builder.Default
    private List<ElectricityBillSummaryResponse> electricityBills = new java.util.ArrayList<>();

    // Module 5
    private Boolean hasSolar;
    private Double  solarCapacityKw;
    private String  certificateUrl;
    private Boolean mnreVerified;

    // Module 6
    private CookingFuel cookingFuelType;
    private String      pngConsumerNumber;
    private Integer     userDeclaredCylinders;
    private Integer     cylinders;
    private String      billUrls;

    // Module 7
    private PublicTransportUsage publicTransportUsage;
    private Boolean              wastesRecycling;

    // Score (when VERIFIED)
    private CarbonScoreResponse carbonScore;

    // Verification outcome (when applicable)
    private String verificationOutcome;
    private String rejectionReason;
    private String agentNotes;
}
