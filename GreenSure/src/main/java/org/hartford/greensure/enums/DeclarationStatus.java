package org.hartford.greensure.enums;

/**
 * Lifecycle states of a CarbonDeclaration.
 * DRAFT → SUBMITTED → UNDER_VERIFICATION → VERIFIED | REJECTED.
 */
public enum DeclarationStatus {

    DRAFT,
    SUBMITTED,
    UNDER_VERIFICATION,
    VERIFIED,
    REJECTED;

    /**
     * Returns true if the user is allowed to edit the declaration.
     * Only DRAFT and REJECTED declarations can be edited.
     */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }

    /**
     * Returns true when the declaration has reached a terminal state
     * (i.e., it will not be re-processed by the system).
     */
    public boolean isTerminal() {
        return this == VERIFIED || this == REJECTED;
    }
}
