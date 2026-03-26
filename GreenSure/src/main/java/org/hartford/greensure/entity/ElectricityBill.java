package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Module 4b — Individual electricity bill record extracted via OCR.
 *
 * One ElectricityBill row is created per uploaded bill.
 * The average of all rows for a declaration is stored as
 * ElectricityData.ocrComputedMonthlyKwh.
 *
 * ocrConfidenceScore (0.0–1.0) indicates how confident the OCR
 * extraction was. Low-confidence bills should be flagged for
 * manual review during agent verification.
 */
@Entity
@Table(name = "electricity_bills")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectricityBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Long billId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    /** Format: "YYYY-MM" e.g. "2024-06". */
    @Column(name = "billing_month", length = 7)
    private String billingMonth;

    /** Units consumed in kWh for this billing period. */
    @Column(name = "units_kwh")
    private Double unitsKwh;

    /** Bill amount in INR. */
    @Column(name = "amount")
    private Double amount;

    /** URL of the uploaded bill PDF or image. */
    @Column(name = "bill_url", columnDefinition = "TEXT")
    private String billUrl;

    /**
     * Confidence score of OCR extraction for this bill.
     * 0.0 = completely unreadable, 1.0 = fully confident.
     */
    @Column(name = "ocr_confidence_score")
    private Double ocrConfidenceScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
