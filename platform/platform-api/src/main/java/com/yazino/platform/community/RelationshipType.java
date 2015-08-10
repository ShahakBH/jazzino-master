package com.yazino.platform.community;

import java.io.Serializable;

public enum RelationshipType implements Serializable {
    FRIEND(new RelationshipAction[]{RelationshipAction.IGNORE, RelationshipAction.REMOVE_FRIEND},
            new RelationshipAction[]{RelationshipAction.IGNORE, RelationshipAction.REMOVE_FRIEND,
                    RelationshipAction.PRIVATE_CHAT}),
    IGNORED(new RelationshipAction[]{RelationshipAction.STOP_IGNORING}),
    IGNORED_BY(new RelationshipAction[]{}),
    IGNORED_FRIEND(new RelationshipAction[]{RelationshipAction.STOP_IGNORING, RelationshipAction.REMOVE_FRIEND}),
    IGNORED_BY_FRIEND(new RelationshipAction[]{RelationshipAction.REMOVE_FRIEND}),
    NOT_FRIEND(new RelationshipAction[]{RelationshipAction.ADD_FRIEND, RelationshipAction.IGNORE}),
    INVITATION_SENT(new RelationshipAction[]{RelationshipAction.ADD_FRIEND, RelationshipAction.IGNORE}),
    INVITATION_RECEIVED(new RelationshipAction[]{RelationshipAction.ADD_FRIEND, RelationshipAction.ACCEPT_FRIEND,
            RelationshipAction.REJECT_FRIEND, RelationshipAction.IGNORE}),
    NO_RELATIONSHIP(new RelationshipAction[]{RelationshipAction.ADD_FRIEND, RelationshipAction.IGNORE});

    public static RelationshipType parse(final String s) {
        return valueOf(s.replace(' ', '_').toUpperCase());
    }

    RelationshipType(final RelationshipAction[] allowedActionsWhenOffline) {
        this.allowedActionsWhenOffline = allowedActionsWhenOffline;
        this.allowedActionsWhenOnline = allowedActionsWhenOffline;
    }

    RelationshipType(final RelationshipAction[] allowedActionsWhenOffline,
                     final RelationshipAction[] allowedActionsWhenOnline) {
        this.allowedActionsWhenOffline = allowedActionsWhenOffline;
        this.allowedActionsWhenOnline = allowedActionsWhenOnline;
    }

    private boolean showOnline;
    private RelationshipAction[] allowedActionsWhenOnline;
    private RelationshipAction[] allowedActionsWhenOffline;

    public RelationshipAction[] getAllowedActions(final boolean isOnline) {
        if (isOnline) {
            return allowedActionsWhenOnline;
        } else {
            return allowedActionsWhenOffline;
        }
    }

}
