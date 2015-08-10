package com.yazino.bi.operations.model.security;

/**
 * User roles for operations access
 */
public enum UserRole {
    ROLE_ROOT("Administrator"), ROLE_MANAGEMENT("Management"), ROLE_MARKETING("Marketing"), ROLE_SUPPORT("Support"),
    ROLE_SUPPORT_MANAGER("Support manager"), ROLE_AD_TRACKING("Ad tracking");

    private final String roleName;

    /**
     * Creates a role enum
     *
     * @param roleName User-readable name for the role
     */
    private UserRole(final String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}
