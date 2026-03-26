package org.hartford.greensure.enums;

/**
 * Cooking fuel type with embedded CO₂ emission rate.
 * The co2PerPersonPerYear value is the annual CO₂ footprint
 * attributed to this fuel type per household member.
 * Embedding this in the enum eliminates magic numbers from
 * service classes.
 */
public enum CookingFuel {

    LPG(300.0,     "LPG Cylinder"),
    PNG(250.0,     "Piped Natural Gas"),
    ELECTRIC(180.0,"Electric Cooking"),
    BIOGAS(20.0,   "Biogas");

    /** kg CO₂ per person per year for this fuel type. */
    private final double co2PerPersonPerYear;

    /** Human-readable display label. */
    private final String label;

    CookingFuel(double co2PerPersonPerYear, String label) {
        this.co2PerPersonPerYear = co2PerPersonPerYear;
        this.label = label;
    }

    public double getCo2PerPersonPerYear() {
        return co2PerPersonPerYear;
    }

    public String getLabel() {
        return label;
    }
}
