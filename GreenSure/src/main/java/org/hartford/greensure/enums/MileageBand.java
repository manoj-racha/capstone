package org.hartford.greensure.enums;

/**
 * Annual mileage bands for vehicle usage.
 * Users select a band instead of entering an exact km value,
 * which reduces the ability to enter implausibly low numbers.
 * Each band maps to a representative annual km figure used in
 * the carbon calculation.
 */
public enum MileageBand {

    BAND_1("Under 5,000 km",       5_000.0),
    BAND_2("5,000–10,000 km",      7_500.0),
    BAND_3("10,000–15,000 km",    12_000.0),
    BAND_4("15,000–20,000 km",    17_000.0),
    BAND_5("Above 20,000 km",     22_000.0);

    /** User-facing label shown in the frontend dropdown. */
    private final String displayLabel;

    /** Representative annual km used in carbon calculation. */
    private final double annualKm;

    MileageBand(String displayLabel, double annualKm) {
        this.displayLabel = displayLabel;
        this.annualKm = annualKm;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public double getAnnualKm() {
        return annualKm;
    }
}
