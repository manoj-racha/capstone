package org.hartford.greensure.enums;

public enum VehicleCategory {
    TWO_WHEELER("2 Wheeler — Bike / Scooter"),
    THREE_WHEELER("3 Wheeler — Auto / E-Rickshaw"),
    FOUR_WHEELER("4 Wheeler — Car / SUV / Van"),
    OTHER("Other — Tractor / Truck / Special");

    private final String displayLabel;

    VehicleCategory(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
