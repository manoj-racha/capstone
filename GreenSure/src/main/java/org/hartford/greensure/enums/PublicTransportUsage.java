package org.hartford.greensure.enums;

/**
 * Frequency of public transport usage declared by the household user.
 * This field is part of the optional Module 7 (Lifestyle Data).
 * The vehicleEmissionReductionPercent is applied as a percentage
 * reduction on the vehicle CO₂ component of the total score.
 * Self-declared — not physically verified by the agent.
 */
public enum PublicTransportUsage {

    NEVER(0.0),
    RARELY(0.0),
    SOMETIMES(1.0),
    OFTEN(3.0),
    ALWAYS(5.0);

    /**
     * Percentage reduction applied to vehicle CO₂ emissions.
     * E.g. ALWAYS = 5% reduction on vehicleCo2.
     */
    private final double vehicleEmissionReductionPercent;

    PublicTransportUsage(double vehicleEmissionReductionPercent) {
        this.vehicleEmissionReductionPercent = vehicleEmissionReductionPercent;
    }

    public double getVehicleEmissionReductionPercent() {
        return vehicleEmissionReductionPercent;
    }
}
