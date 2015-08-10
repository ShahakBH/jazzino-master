package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class NewsEvent implements Serializable {
    private static final long serialVersionUID = 250091073309893046L;

    private final BigDecimal playerId;
    private final ParameterisedMessage news;
    private final String title;
    private final String postedAchievementTitleText;
    private final String postedAchievementTitleLink;
    private final String postedAchievementActionText;
    private final String postedAchievementActionLink;
    private final String image;
    private final long delay;
    private final NewsEventType type;
    private final ParameterisedMessage shortDescription;
    private final String gameType;

    private NewsEvent(final BigDecimal playerId,
                      final NewsEventType type,
                      final ParameterisedMessage news,
                      final String title,
                      final String postedAchievementTitleText,
                      final String postedAchievementTitleLink,
                      final String postedAchievementActionText,
                      final String postedAchievementActionLink,
                      final ParameterisedMessage shortDescription,
                      final String image,
                      final long delay,
                      final String gameType) {
        notNull(playerId, "Player ID may not be null");
        notNull(news, "News may not be null");
        notNull(image, "Image may not be null");
        notNull(type, "Type may not be null");

        this.playerId = playerId;
        this.type = type;
        this.news = news;
        this.title = title;
        this.postedAchievementTitleText = postedAchievementTitleText;
        this.postedAchievementTitleLink = postedAchievementTitleLink;
        this.postedAchievementActionText = postedAchievementActionText;
        this.postedAchievementActionLink = postedAchievementActionLink;
        this.shortDescription = shortDescription;
        this.image = image;
        this.delay = delay;
        this.gameType = gameType;
    }

    private void notNull(final Object object,
                         final String errorMessage) {
        if (object == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }


    public ParameterisedMessage getNews() {
        return news;
    }


    public long getDelay() {
        return delay;
    }

    public String getImage() {
        return image;
    }


    public NewsEventType getType() {
        return type;
    }


    public ParameterisedMessage getShortDescription() {
        return shortDescription;
    }

    public String getGameType() {
        return gameType;
    }

    public String getTitle() {
        return title;
    }

    public String getPostedAchievementTitleText() {
        return postedAchievementTitleText;
    }

    public String getPostedAchievementTitleLink() {
        return postedAchievementTitleLink;
    }

    public String getPostedAchievementActionText() {
        return postedAchievementActionText;
    }

    public String getPostedAchievementActionLink() {
        return postedAchievementActionLink;
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

        final NewsEvent rhs = (NewsEvent) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(type, rhs.type)
                .append(news, rhs.news)
                .append(title, rhs.title)
                .append(shortDescription, rhs.shortDescription)
                .append(image, rhs.image)
                .append(delay, rhs.delay)
                .append(gameType, rhs.gameType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 47)
                .append(playerId)
                .append(type)
                .append(news)
                .append(title)
                .append(shortDescription)
                .append(image)
                .append(delay)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        private BigDecimal playerId;
        private NewsEventType type = NewsEventType.NEWS;
        private ParameterisedMessage news;
        private String title;
        private String postedAchievementTitleText;
        private String postedAchievementTitleLink;
        private String postedAchievementActionText;
        private String postedAchievementActionLink;
        private ParameterisedMessage shortDescription;
        private String image = "";
        private long delay;
        private String gameType;

        public Builder(final BigDecimal playerId,
                       final ParameterisedMessage news) {
            this.playerId = playerId;
            this.news = news;
        }

        public Builder setImage(final String newImage) {
            this.image = newImage;
            return this;
        }

        public Builder setType(final NewsEventType newType) {
            this.type = newType;
            return this;
        }

        public Builder setShortDescription(final ParameterisedMessage newShortDescription) {
            this.shortDescription = newShortDescription;
            return this;
        }

        public Builder setDelay(final long newDelay) {
            this.delay = newDelay;
            return this;
        }

        public Builder setGameType(final String newGameType) {
            this.gameType = newGameType;
            return this;
        }

        public Builder setTitle(final String newTitle) {
            this.title = newTitle;
            return this;
        }

        public Builder setPostedAchievementTitleText(final String newPostedAchievementTitleText) {
            this.postedAchievementTitleText = newPostedAchievementTitleText;
            return this;
        }

        public Builder setPostedAchievementTitleLink(final String newPostedAchievementTitleLink) {
            this.postedAchievementTitleLink = newPostedAchievementTitleLink;
            return this;
        }

        public Builder setPostedAchievementActionText(final String newPostedAchievementActionText) {
            this.postedAchievementActionText = newPostedAchievementActionText;
            return this;
        }

        public Builder setPostedAchievementActionLink(final String newPostedAchievementActionLink) {
            this.postedAchievementActionLink = newPostedAchievementActionLink;
            return this;
        }

        public NewsEvent build() {
            return new NewsEvent(playerId,
                    type,
                    news,
                    title,
                    postedAchievementTitleText,
                    postedAchievementTitleLink,
                    postedAchievementActionText,
                    postedAchievementActionLink,
                    shortDescription,
                    image,
                    delay,
                    gameType);
        }
    }
}
