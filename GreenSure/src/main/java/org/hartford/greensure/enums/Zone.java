package org.hartford.greensure.enums;

/**
 * Carbon zone classification based on per-capita CO₂ (kg/year).
 * Thresholds are per-person per year for household users.
 * Each zone carries the maximum CO₂ threshold for that zone and the
 * maximum discount percentage this zone unlocks.
 */
public enum Zone {

    GREEN_CHAMPION(1500.0, 30.0),
    IMPROVER(4000.0, 15.0),
    DEFAULTER(Double.MAX_VALUE, 0.0);

    private final double maxCo2;
    private final double zoneDiscountPercent;

    Zone(double maxCo2, double zoneDiscountPercent) {
        this.maxCo2 = maxCo2;
        this.zoneDiscountPercent = zoneDiscountPercent;
    }

    public double getMaxCo2() {
        return maxCo2;
    }

    public double getZoneDiscountPercent() {
        return zoneDiscountPercent;
    }

    /**
     * Classifies a per-capita CO₂ value into the correct zone.
     * GREEN_CHAMPION : totalCo2 &lt; 1,500 kg/year per person
     * IMPROVER       : 1,500 – 4,000 kg/year per person
     * DEFAULTER      : above 4,000 kg/year per person
     */
    public static Zone fromCo2(double perCapitaCo2) {
        if (perCapitaCo2 < GREEN_CHAMPION.maxCo2) return GREEN_CHAMPION;
        if (perCapitaCo2 < IMPROVER.maxCo2)       return IMPROVER;
        return DEFAULTER;
    }
}
