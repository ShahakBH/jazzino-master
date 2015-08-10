package com.yazino.web.controller.profile;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ProfileTab {

    private String label;
    private String href;
    private String codeName;

    public ProfileTab(final String codeName, final String label, final String href) {
        this.codeName = codeName;
        this.label = label;
        this.href = href;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getLabel() {
        return label;
    }

    public String getHref() {
        return href;
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
        final ProfileTab rhs = (ProfileTab) obj;
        return new EqualsBuilder()
                .append(label, rhs.label)
                .append(href, rhs.href)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(label)
                .append(href)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(label)
                .append(href)
                .toString();
    }

}
