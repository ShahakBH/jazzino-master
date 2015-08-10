package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

public class Avatar implements Serializable, Comparable<Avatar> {
    private static final long serialVersionUID = -5555396142297883736L;

    private final String pictureLocation;
    private final String url;

    public Avatar(final String pictureLocation,
                  final String url) {
        notNull(pictureLocation, "Picture location is required");
        notNull(url, "URL is required");

        this.pictureLocation = pictureLocation;
        this.url = url;
    }

    public String getPictureLocation() {
        return pictureLocation;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Avatar rhs = (Avatar) o;
        return new EqualsBuilder()
                .append(pictureLocation, rhs.pictureLocation)
                .append(url, rhs.url)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(pictureLocation)
                .append(url)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(pictureLocation)
                .append(url)
                .toString();
    }

    public int compareTo(final Avatar avatar) {
        return pictureLocation.compareTo(avatar.pictureLocation);
    }

}
