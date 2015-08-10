package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

public class ReelsRotate implements NovomaticEvent {
    private final List<String> reels;

    public List<String> getReels() {
        return reels;
    }

    public ReelsRotate(List<String> reels) {
        this.reels = reels;
    }

    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventReelsRotate.getNovomaticEventType();
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
        ReelsRotate rhs = (ReelsRotate) obj;
        return new EqualsBuilder()
                .append(this.reels, rhs.reels)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(reels)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
