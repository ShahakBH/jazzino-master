package com.yazino.platform.community;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class Trophy implements Serializable {
    private static final long serialVersionUID = -8268056418672421026L;

    private BigDecimal id;
    private String name;
    private String image;
    private String gameType;
    private String message;
    private String shortDescription;
    private String messageCabinet;

    public Trophy() {
    }

    public Trophy(final BigDecimal id,
                  final String name,
                  final String gameType,
                  final String image) {
        this.gameType = gameType;
        notNull(id, "ID may not be null");
        notBlank(name, "Name may not be blank");
        notBlank(gameType, "gameType may not be blank");

        this.id = id;
        this.name = name;
        this.image = image;
    }

    public BigDecimal getId() {
        return id;
    }

    public void setId(final BigDecimal id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(final String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getMessageCabinet() {
        return messageCabinet;
    }

    public void setMessageCabinet(final String messageCabinet) {
        this.messageCabinet = messageCabinet;
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

        final Trophy rhs = (Trophy) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(gameType, rhs.gameType)
                .append(image, rhs.image)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 51)
                .append(id)
                .append(name)
                .append(gameType)
                .append(image)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
