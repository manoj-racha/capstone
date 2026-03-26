package org.hartford.greensure.enums;

/**
 * Source from which vehicle data was retrieved.
 * VAHAN and DIGILOCKER are considered system-verified;
 * MANUAL requires physical document check by the field agent.
 */
public enum DataSource {

    VAHAN(true,      "VAHAN API Lookup"),
    DIGILOCKER(true, "DigiLocker"),
    MANUAL(false,    "Manual Entry");

    /** True if the data came from an authoritative API/document service. */
    private final boolean verified;

    /** Human-readable label. */
    private final String label;

    DataSource(boolean verified, String label) {
        this.verified = verified;
        this.label = label;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getLabel() {
        return label;
    }
}
