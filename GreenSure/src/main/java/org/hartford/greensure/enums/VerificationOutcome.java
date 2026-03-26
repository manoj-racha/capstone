package org.hartford.greensure.enums;

/**
 * The outcome decision made by the field agent during a physical visit.
 *
 * CONFIRMED — agent found all declared values to be accurate
 * MODIFIED  — agent corrected one or more values; corrected values are stored
 *             in the agentCorrected* fields of the module entities and are
 *             used in the carbon calculation via getEffective*() methods
 * REJECTED  — agent rejected the declaration; user is notified with reason
 *             and may resubmit (up to 3 times)
 */
public enum VerificationOutcome {
    CONFIRMED,
    MODIFIED,
    REJECTED
}
