package org.hartford.greensure.enums;

public enum VehicleDocumentType {
    RC_BOOK("Registration Certificate"),
    INSURANCE("Vehicle Insurance"),
    POLLUTION_CERTIFICATE("Pollution Under Control"),
    OTHER("Other Document");

    private final String displayLabel;

    VehicleDocumentType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
