package com.yazino.platform.model;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class DataLoadComplete implements Serializable {
    private static final long serialVersionUID = -2141071394471767068L;

    private Integer spaceRouting;
    private String loaderName;

    public DataLoadComplete() {
    }

    public DataLoadComplete(final String loaderName) {
        notNull(loaderName, "loaderName may not be null");

        this.loaderName = loaderName;
    }

    public DataLoadComplete(final int spaceRouting,
                            final String loaderName) {
        notNull(loaderName, "loaderName may not be null");

        this.spaceRouting = spaceRouting;
        this.loaderName = loaderName;
    }

    @SpaceRouting
    public Integer getSpaceRouting() {
        return spaceRouting;
    }

    public void setSpaceRouting(final Integer spaceRouting) {
        this.spaceRouting = spaceRouting;
    }

    @SpaceId
    public String getLoaderName() {
        return loaderName;
    }

    public void setLoaderName(final String loaderName) {
        this.loaderName = loaderName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DataLoadComplete rhs = (DataLoadComplete) obj;
        return new EqualsBuilder()
                .append(this.spaceRouting, rhs.spaceRouting)
                .append(this.loaderName, rhs.loaderName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceRouting)
                .append(loaderName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("spaceRouting", spaceRouting)
                .append("loaderName", loaderName)
                .toString();
    }
}
