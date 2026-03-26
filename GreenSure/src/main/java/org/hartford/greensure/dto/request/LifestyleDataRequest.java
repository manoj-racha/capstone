package org.hartford.greensure.dto.request;

import lombok.*;
import org.hartford.greensure.enums.PublicTransportUsage;

/** Request DTO for Module 7 — Lifestyle Data (optional bonus module). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LifestyleDataRequest {

    /** Defaults to NEVER if not provided. */
    private PublicTransportUsage publicTransportUsage = PublicTransportUsage.NEVER;

    /** Grants a flat 50 kg CO₂ reduction. */
    private boolean wastesRecycling = false;
}
