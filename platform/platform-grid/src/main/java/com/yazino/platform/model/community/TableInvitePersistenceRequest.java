package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@SpaceClass(replicate = false)
public class TableInvitePersistenceRequest implements Serializable {

    private static final long serialVersionUID = 1291291800393207065L;

    private TableInvite tableInvite;
    private String spaceId;

    @SpaceId
    @SpaceRouting
    public final String getSpaceId() {
        return spaceId;
    }

    public final void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public TableInvitePersistenceRequest() {
        this.tableInvite = null;
        this.spaceId = null;
    }

    public TableInvitePersistenceRequest(final TableInvite tableInvite) {
        this.tableInvite = tableInvite;
        this.spaceId = tableInvite.getId().toString();
    }

    public TableInvite getTableInvite() {
        return tableInvite;
    }

    public void setTableInvite(final TableInvite tableInvite) {
        this.tableInvite = tableInvite;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TableInvitePersistenceRequest rhs = (TableInvitePersistenceRequest) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(tableInvite, rhs.tableInvite)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(tableInvite)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
