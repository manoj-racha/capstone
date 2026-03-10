package org.hartford.greensure.engine;


public class EmissionFactors {

    // ── ELECTRICITY ────────────────────────────────────────────
    // India average grid emission factor
    public static final double ELECTRICITY_KG_PER_KWH = 0.82;

    // ── COOKING FUEL ───────────────────────────────────────────
    // LPG: 14.2 kg per cylinder × 2.983 kg CO2 per kg
    public static final double LPG_KG_CO2_PER_CYLINDER = 42.36;

    // PNG: per SCM (Standard Cubic Meter)
    public static final double PNG_KG_CO2_PER_SCM = 2.02;

    // Biomass: per kg per day
    public static final double BIOMASS_KG_CO2_PER_KG = 1.83;

    // Kerosene: per litre
    public static final double KEROSENE_KG_CO2_PER_LITRE = 2.54;

    // ── AC UNITS ───────────────────────────────────────────────
    // Average AC power consumption: 1.5 kW per unit
    // CO2 per hour = 1.5 kWh × 0.82
    public static final double AC_KG_CO2_PER_UNIT_PER_HOUR = 1.23;

    // ── GENERATOR ─────────────────────────────────────────────
    // Average diesel generator: 1 litre per hour
    // Diesel CO2 factor: 2.68 kg per litre
    public static final double GENERATOR_KG_CO2_PER_HOUR = 2.68;

    // Generator diesel factor for MSME (per litre)
    public static final double DIESEL_KG_CO2_PER_LITRE = 2.68;

    // ── TWO WHEELERS ───────────────────────────────────────────
    public static final double TWO_WHEELER_PETROL_KG_PER_KM = 0.047;
    public static final double TWO_WHEELER_DIESEL_KG_PER_KM = 0.055;
    public static final double TWO_WHEELER_ELECTRIC_KG_PER_KM = 0.020;

    // ── FOUR WHEELERS ──────────────────────────────────────────
    public static final double FOUR_WHEELER_PETROL_KG_PER_KM = 0.192;
    public static final double FOUR_WHEELER_DIESEL_KG_PER_KM = 0.196;
    public static final double FOUR_WHEELER_CNG_KG_PER_KM = 0.165;
    public static final double FOUR_WHEELER_ELECTRIC_KG_PER_KM = 0.058;

    // ── COMMERCIAL VEHICLES ────────────────────────────────────
    public static final double COMMERCIAL_DIESEL_KG_PER_KM = 0.450;
    public static final double COMMERCIAL_CNG_KG_PER_KM = 0.380;

    // ── PUBLIC TRANSPORT ──────────────────────────────────────
    public static final double PUBLIC_BUS_KG_PER_KM = 0.039;

    // ── THIRD PARTY LOGISTICS ─────────────────────────────────
    // Per shipment average estimate
    public static final double LOGISTICS_KG_CO2_PER_SHIPMENT = 2.50;

    // ── DIET — kg CO2 per person per year ─────────────────────
    public static final double DIET_VEGAN = 720.0;
    public static final double DIET_VEGETARIAN = 1000.0;
    public static final double DIET_EGGETARIAN = 1300.0;
    public static final double DIET_NON_VEGETARIAN = 1700.0;
    public static final double DIET_HEAVY_NON_VEGETARIAN = 2500.0;

    // ── ONLINE SHOPPING — kg CO2 per year ─────────────────────
    public static final double SHOPPING_ZERO_TO_FIVE = 20.0;
    public static final double SHOPPING_SIX_TO_FIFTEEN = 60.0;
    public static final double SHOPPING_ABOVE_FIFTEEN = 120.0;

    // ── BOILER ────────────────────────────────────────────────
    public static final double BOILER_COAL_KG_CO2_PER_KG = 2.42;
    public static final double BOILER_GAS_KG_CO2_PER_SCM = 2.02;

    // ── PAPER ─────────────────────────────────────────────────
    // Per ream (500 sheets)
    public static final double PAPER_KG_CO2_PER_REAM = 2.45;
    // Recycled paper has 50% lower footprint
    public static final double PAPER_RECYCLED_FACTOR = 0.50;

    // ── RAW MATERIAL ──────────────────────────────────────────
    // Per kg — virgin material
    public static final double RAW_MATERIAL_VIRGIN_KG_CO2_PER_KG
            = 3.50;
    // Per kg — recycled material
    public static final double RAW_MATERIAL_RECYCLED_KG_CO2_PER_KG
            = 1.20;
    // Per kg — mixed material (average)
    public static final double RAW_MATERIAL_MIXED_KG_CO2_PER_KG
            = 2.35;

    // ── ZONE THRESHOLDS ───────────────────────────────────────

    // Household per capita kg CO2 per year
    public static final double HH_GREEN_CHAMPION_THRESHOLD = 1500.0;
    public static final double HH_GREEN_IMPROVER_THRESHOLD = 2500.0;
    // Above 2500 = GREEN_DEFAULTER

    // MSME per employee kg CO2 per year
    public static final double MSME_GREEN_CHAMPION_THRESHOLD = 2500.0;
    public static final double MSME_GREEN_IMPROVER_THRESHOLD = 5000.0;
    // Above 5000 = GREEN_DEFAULTER

    // ── MONTHS IN YEAR ────────────────────────────────────────
    public static final int MONTHS_IN_YEAR = 12;
    public static final int DAYS_IN_YEAR = 365;
}
