package com.yazino.platform.opengraph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class OpenGraphAction implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("object")
    private OpenGraphObject object;

    public OpenGraphAction() {
    }

    public OpenGraphAction(final String name,
                           final OpenGraphObject object) {
        this.name = name;
        this.object = object;
    }

    public String getName() {
        return name;
    }

    public OpenGraphObject getObject() {
        return object;
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
        final OpenGraphAction rhs = (OpenGraphAction) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(object, rhs.object)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(name)
                .append(object)
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
