package com.yazino.platform.model.chat;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@SpaceClass
public class ChatChannel implements Serializable {
    private static final long serialVersionUID = 7050250470792458775L;
    private String channelId;
    private ChatChannelType channelType;
    private String locationId;

    public ChatChannel() {
        //for gigaspace
    }

    public ChatChannel(final String channelId) {
        this.channelId = channelId;
        //for template
    }

    public ChatChannel(final ChatChannelType channelType) {
        this.channelType = channelType;
    }

    public ChatChannel(final ChatChannelType channelType,
                       final String locationId) {
        this.channelType = channelType;
        this.locationId = locationId;
    }

    @SpaceRouting
    @SpaceId(autoGenerate = false)
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    public ChatChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(final ChatChannelType channelType) {
        this.channelType = channelType;
    }

    @SpaceIndex
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(final String locationId) {
        this.locationId = locationId;
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
        final ChatChannel rhs = (ChatChannel) obj;
        return new EqualsBuilder()
                .append(channelId, rhs.channelId)
                .append(channelType, rhs.channelType)
                .append(locationId, rhs.locationId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(channelId)
                .append(channelType)
                .append(locationId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(channelId)
                .append(channelType)
                .append(locationId)
                .toString();
    }

}
