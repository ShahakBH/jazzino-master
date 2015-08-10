package com.yazino.bi.operations.security;

/**
 * User roles for operations access
 *
 * This should be replaced by the OPERATION_ROLE table.
 */
public enum UserRole {
    ROLE_ROOT("Administrator"),
    ROLE_MANAGEMENT("Management"),
    ROLE_MARKETING("Marketing"),
    ROLE_SUPPORT("Support"),
    ROLE_SUPPORT_MANAGER("Support manager"),
    ROLE_AD_TRACKING("Ad tracking"),
    ROLE_INGAMING_TRACKING("InGaming Ad Tracking"),
    ROLE_GAME("Game"),
    ROLE_GLOBAL_ADMIN("Global Administrator"),
    ROLE_PARTNER_ADMIN("Partner Administrator"),
    ROLE_PAYMENTS("Payments"),
    ROLE_REPORTING("Reporting");

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
