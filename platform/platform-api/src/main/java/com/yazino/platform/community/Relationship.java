package com.yazino.platform.community;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public final class Relationship implements Serializable {
    private static final long serialVersionUID = -6391554322146208644L;

    private String nickname;
    private RelationshipType type;

    public Relationship(final String nickname,
                        final RelationshipType type) {
        this.nickname = nickname;
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public RelationshipType getType() {
        return type;
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
        final Relationship rhs = (Relationship) obj;
        return new EqualsBuilder()
                .append(nickname, rhs.nickname)
                .append(type, rhs.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(nickname)
                .append(type)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(nickname)
                .append(type)
                .toString();
    }
}
