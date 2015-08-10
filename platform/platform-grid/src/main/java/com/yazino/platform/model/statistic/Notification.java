package com.yazino.platform.model.statistic;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Notification implements Serializable {
    private static final long serialVersionUID = 250091073309893046L;
    protected static final String DEFAULT_ACTION_TEXT = "Yazino - Let's Play!";
    protected static final String DEFAULT_ACTION_LINK = "http://www.yazino.com";

    private final BigDecimal playerId;
    private final NotificationMessage news;
    private final String title;
    private final String postedAchievementTitleText;
    private final String postedAchievementTitleLink;
    private final String postedAchievementActionText;
    private final String postedAchievementActionLink;
    private final String image;
    private final long delay;
    private final NotificationType type;
    private final NotificationMessage shortDescription;
    private final String gameType;

    private Notification(final BigDecimal playerId,
                         final NotificationType type,
                         final NotificationMessage news,
                         final String title,
                         final String postedAchievementTitleText,
                         final String postedAchievementTitleLink,
                         final String postedAchievementActionText,
                         final String postedAchievementActionLink,
                         final NotificationMessage shortDescription,
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

    public NotificationMessage getNews() {
        return news;
    }

    public long getDelay() {
        return delay;
    }


    public String getImage() {
        return image;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationMessage getShortDescription() {
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

        final Notification rhs = (Notification) obj;
        return new EqualsBuilder()
                .append(news, rhs.news)
                .append(title, rhs.title)
                .append(postedAchievementTitleText, rhs.postedAchievementTitleText)
                .append(postedAchievementTitleLink, rhs.postedAchievementTitleLink)
                .append(postedAchievementActionText, rhs.postedAchievementActionText)
                .append(postedAchievementActionLink, rhs.postedAchievementActionLink)
                .append(image, rhs.image)
                .append(delay, rhs.delay)
                .append(type, rhs.type)
                .append(shortDescription, rhs.shortDescription)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(news)
                .append(title)
                .append(postedAchievementTitleText)
                .append(postedAchievementTitleLink)
                .append(postedAchievementActionText)
                .append(postedAchievementActionLink)
                .append(image)
                .append(delay)
                .append(type)
                .append(shortDescription)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        private BigDecimal playerId;
        private NotificationType type = NotificationType.UNKNOWN;
        private NotificationMessage news;
        private String title;
        private String postedAchievementTitleText = DEFAULT_ACTION_TEXT;
        private String postedAchievementTitleLink = DEFAULT_ACTION_LINK;
        private String postedAchievementActionText = DEFAULT_ACTION_TEXT;
        private String postedAchievementActionLink = DEFAULT_ACTION_LINK;
        private NotificationMessage shortDescription;
        private String image = "";
        private long delay;
        private String gameType;

        public Builder(final BigDecimal playerId, final NotificationMessage news) {
            this.playerId = playerId;
            this.news = news;
        }

        public Builder setImage(final String newImage) {
            this.image = newImage;
            return this;
        }

        public Builder setType(final NotificationType newType) {
            this.type = newType;
            return this;
        }

        public Builder setShortDescription(final NotificationMessage newShortDescription) {
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
            if (StringUtils.isNotBlank(newPostedAchievementTitleText)) {
                this.postedAchievementTitleText = newPostedAchievementTitleText;
            }
            return this;
        }

        public Builder setPostedAchievementTitleLink(final String newPostedAchievementTitleLink) {
            if (StringUtils.isNotBlank(newPostedAchievementTitleLink)) {
                this.postedAchievementTitleLink = newPostedAchievementTitleLink;
            }
            return this;
        }

        public Builder setPostedAchievementActionText(final String newPostedAchievementActionText) {
            if (StringUtils.isNotBlank(newPostedAchievementActionText)) {
                this.postedAchievementActionText = newPostedAchievementActionText;
            }
            return this;
        }

        public Builder setPostedAchievementActionLink(final String newPostedAchievementActionLink) {
            if (StringUtils.isNotBlank(newPostedAchievementActionLink)) {
                this.postedAchievementActionLink = newPostedAchievementActionLink;
            }
            return this;
        }

        public Notification build() {
            return new Notification(playerId,
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
