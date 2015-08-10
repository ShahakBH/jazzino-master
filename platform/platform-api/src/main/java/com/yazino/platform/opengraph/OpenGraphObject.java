package com.yazino.platform.opengraph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;


public class OpenGraphObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    public OpenGraphObject() {
    }

    public OpenGraphObject(final String type,
                           final String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
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
        final OpenGraphObject rhs = (OpenGraphObject) obj;
        return new EqualsBuilder()
                .append(type, rhs.type)
                .append(id, rhs.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(type)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
