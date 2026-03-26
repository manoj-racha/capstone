package org.hartford.greensure.service;

import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.DataSource;
import org.hartford.greensure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Advisory-only fraud detection engine.
 *
 * Checks the declaration against 5 rules that may indicate potential fraud.
 * NEVER blocks submission — only sets an advisory risk level visible to the agent.
 *
 * Rules:
 *   R1 — MANUAL vehicle data source but no RC upload URL
 *   R2 — EV declared but no electricity bill OR very low monthly kWh (&lt; 50)
 *   R3 — Solar installation claimed but no certificate URL
 *   R4 — Electricity is very high (&gt; 1500 kWh/month) — outlier detection
 *   R5 — New vehicle (year > current - 2) but mileage band is BAND_4 or BAND_5
 *
 * Risk level:
 *   0–1 flags → LOW
 *   2–3 flags → MEDIUM
 *   4+  flags → HIGH
 */
@Service
public class FraudAdvisoryService {

    private static final Logger log = LoggerFactory.getLogger(FraudAdvisoryService.class);

    @Autowired private DeclarationVehicleDataRepository vehicleDataRepo;
    @Autowired private ElectricityDataRepository electricityDataRepo;
    @Autowired private SolarDataRepository solarDataRepo;

    public record FraudAdvisoryResult(int score, List<String> flags, String riskLevel) {}

    /**
     * Analyzes the declaration and returns an advisory result.
     * Never throws — if an error occurs, returns LOW risk.
     */
    public FraudAdvisoryResult analyze(Long declarationId) {
        try {
            return doAnalyze(declarationId);
        } catch (Exception ex) {
            log.warn("FraudAdvisory failed for declaration {}: {}", declarationId, ex.getMessage());
            return new FraudAdvisoryResult(0, List.of(), "LOW");
        }
    }

    private FraudAdvisoryResult doAnalyze(Long declarationId) {
        List<String> flags = new ArrayList<>();

        List<DeclarationVehicleData> vehicles = vehicleDataRepo.findByDeclarationDeclarationId(declarationId);

        if (!vehicles.isEmpty()) {
            // R1 / R3 — MANUAL source but no document
            boolean hasManualNoDoc = vehicles.stream().anyMatch(v -> 
                v.getDataSource() == DataSource.MANUAL && (v.getDocuments() == null || v.getDocuments().isEmpty())
            );
            if (hasManualNoDoc) {
                flags.add("MANUAL_VEHICLE_NO_DOCUMENT");
            }

            // R5 — Brand-new vehicle with very high mileage band
            boolean hasNewHighMileage = vehicles.stream().anyMatch(v -> {
                int currentYear = java.time.Year.now().getValue();
                return v.getManufacturingYear() != null
                        && v.getManufacturingYear() > currentYear - 2
                        && v.getMileageBand() != null
                        && (v.getMileageBand() == org.hartford.greensure.enums.MileageBand.BAND_4
                            || v.getMileageBand() == org.hartford.greensure.enums.MileageBand.BAND_5);
            });
            if (hasNewHighMileage) {
                flags.add("NEW_VEHICLE_HIGH_MILEAGE");
            }

            // R2 — EV but low electricity or no bills
            boolean hasEV = vehicles.stream().allMatch(v -> v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.EV);
            if (hasEV) {
                electricityDataRepo.findByDeclarationDeclarationId(declarationId).ifPresentOrElse(
                    e -> {
                        double kwh = e.getEffectiveMonthlyKwh();
                        if (kwh < 50.0) flags.add("EV_DECLARED_LOW_ELECTRICITY");
                    },
                    () -> flags.add("EV_DECLARED_NO_ELECTRICITY_DATA")
                );
            }
        }

        // R3 — Solar claimed but no certificate
        solarDataRepo.findByDeclarationDeclarationId(declarationId).ifPresent(s -> {
            if (s.isHasSolar()
                    && (s.getCertificateUrl() == null || s.getCertificateUrl().isBlank())) {
                flags.add("SOLAR_CLAIMED_NO_CERTIFICATE");
            }
        });

        // R4 — Electricity outlier (> 1500 kWh/month)
        electricityDataRepo.findByDeclarationDeclarationId(declarationId).ifPresent(e -> {
            double kwh = e.getEffectiveMonthlyKwh();
            if (kwh > 1500.0) flags.add("ELECTRICITY_OUTLIER_HIGH");
        });

        int score = flags.size();
        String riskLevel;
        if (score >= 4)      riskLevel = "HIGH";
        else if (score >= 2) riskLevel = "MEDIUM";
        else                 riskLevel = "LOW";

        return new FraudAdvisoryResult(score, flags, riskLevel);
    }
}
