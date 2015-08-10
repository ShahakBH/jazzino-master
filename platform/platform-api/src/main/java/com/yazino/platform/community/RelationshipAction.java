package com.yazino.platform.community;


public enum RelationshipAction {
    ADD_FRIEND,
    REMOVE_FRIEND,
    IGNORE,
    STOP_IGNORING,
    PRIVATE_CHAT,
    SET_EXTERNAL_FRIEND,
    ACCEPT_FRIEND,
    REJECT_FRIEND;

    public static RelationshipAction parse(final String s) {
        return valueOf(s.replace(' ', '_').toUpperCase());
    }
}
