package org.hartford.greensure.enums;

/**
 * Vehicle fuel type with associated CO₂ emission factor.
 * Emission factors are in kg CO₂ per km driven.
 * Source: India-specific averages for passenger vehicles.
 */
public enum FuelType {

    EV(0.08,     "Electric Vehicle"),
    PETROL(0.21, "Petrol"),
    DIESEL(0.27, "Diesel"),
    CNG(0.14,    "CNG");

    /** kg CO₂ emitted per km driven. */
    private final double emissionFactor;

    /** Human-readable display label. */
    private final String label;

    FuelType(double emissionFactor, String label) {
        this.emissionFactor = emissionFactor;
        this.label = label;
    }

    public double getEmissionFactor() {
        return emissionFactor;
    }

    public String getLabel() {
        return label;
    }
}
