package org.hartford.greensure.enums;

/**
 * Platform roles. USER = household policy-seeker,
 * AGENT = field verification agent, ADMIN = platform admin.
 * Each value carries its Spring Security authority string.
 */
public enum Role {

    USER("ROLE_USER"),
    AGENT("ROLE_AGENT"),
    ADMIN("ROLE_ADMIN");

    private final String springRole;

    Role(String springRole) {
        this.springRole = springRole;
    }

    /** Returns the Spring Security authority string (e.g. "ROLE_USER"). */
    public String getSpringRole() {
        return springRole;
    }
}
