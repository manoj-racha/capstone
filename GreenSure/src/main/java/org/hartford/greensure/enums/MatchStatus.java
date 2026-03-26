package org.hartford.greensure.enums;

/**
 * Match status used in the per-field comparison table shown to the
 * field agent inside the verification workspace.
 * Each row of the comparison table gets one MatchStatus value.
 */
public enum MatchStatus {

    MATCH("✓ Match",            "Declared value matches the system-verified value"),
    MISMATCH("⚠ Mismatch",      "Declared value conflicts with the system-verified value"),
    UNVERIFIED("? Unverified",  "System could not independently verify this field"),
    NOT_APPLICABLE("— N/A",     "Field is not applicable for this user");

    private final String displayLabel;
    private final String description;

    MatchStatus(String displayLabel, String description) {
        this.displayLabel = displayLabel;
        this.description = description;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getDescription() {
        return description;
    }
}
