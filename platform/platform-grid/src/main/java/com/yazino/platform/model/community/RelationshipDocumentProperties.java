package com.yazino.platform.model.community;

import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class RelationshipDocumentProperties implements Serializable {
    private static final long serialVersionUID = 2789256947220255006L;
    private String nickname;
    private String pictureUrl;
    private RelationshipType relationshipType;
    private PlayerSessionDocumentProperties status;
    private RelationshipAction[] allowedActions;

    public RelationshipAction[] getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(final RelationshipAction[] allowedActions) {
        this.allowedActions = allowedActions;
    }

    public String getNickname() {
        return nickname;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public PlayerSessionDocumentProperties getStatus() {
        return status;
    }

    public RelationshipDocumentProperties(final String nickname,
                                          final String pictureUrl,
                                          final RelationshipType relationshipType,
                                          final PlayerSessionDocumentProperties status,
                                          final boolean isOnline) {
        this.nickname = nickname;
        this.pictureUrl = pictureUrl;
        this.relationshipType = relationshipType;
        this.status = status;
        this.allowedActions = relationshipType.getAllowedActions(isOnline);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final RelationshipDocumentProperties rhs = (RelationshipDocumentProperties) obj;
        return new EqualsBuilder()
                .append(allowedActions, rhs.allowedActions)
                .append(nickname, rhs.nickname)
                .append(pictureUrl, rhs.pictureUrl)
                .append(relationshipType, rhs.relationshipType)
                .append(status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(allowedActions)
                .append(nickname)
                .append(pictureUrl)
                .append(relationshipType)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(allowedActions)
                .append(nickname)
                .append(pictureUrl)
                .append(relationshipType)
                .append(status)
                .toString();
    }

}
