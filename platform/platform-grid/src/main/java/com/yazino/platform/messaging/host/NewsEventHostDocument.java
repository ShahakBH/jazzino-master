package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class NewsEventHostDocument implements HostDocument {
    private static final long serialVersionUID = -7174827235103245815L;

    private static final String DEFAULT_POSTED_ACHIEVEMENT_TEXT = "Yazino - Let's Play!";
    private static final String DEFAULT_POSTED_ACHIEVEMENT_TITLE_LINK = "http://www.yazino.com";

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final BigDecimal tableId;
    private final String partnerId;
    private final NewsEvent newsEvent;
    private final Destination destination;

    public NewsEventHostDocument(final NewsEvent newsEvent,
                                 final Destination destination) {
        notNull(newsEvent, "newsEvent may not be null");
        notNull(destination, "destination may not be null");

        this.newsEvent = newsEvent;
        this.destination = destination;

        this.tableId = null;
        this.partnerId = null;
    }

    public NewsEventHostDocument(final String partnerId,
                                 final NewsEvent newsEvent,
                                 final Destination destination) {
        notNull(partnerId, "partnerId may not be null");
        notNull(newsEvent, "newsEvent may not be null");
        notNull(destination, "destination may not be null");

        this.partnerId = partnerId;
        this.newsEvent = newsEvent;
        this.destination = destination;

        this.tableId = null;
    }

    public NewsEventHostDocument(final BigDecimal tableId,
                                 final String partnerId,
                                 final NewsEvent newsEvent,
                                 final Destination destination) {
        notNull(tableId, "tableId may not be null");
        notNull(partnerId, "partnerId may not be null");
        notNull(newsEvent, "newsEvent may not be null");
        notNull(destination, "destination may not be null");

        this.tableId = tableId;
        this.partnerId = partnerId;
        this.newsEvent = newsEvent;
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        destination.send(new Document(DocumentType.NEWS_EVENT.getName(), body(), headers()), documentDispatcher);
    }

    public String body() {
        final ParameterisedMessage formattedMessage = new ParameterisedMessage(newsEvent.getNews().toString());
        ParameterisedMessage formattedShortDesc = null;
        if (newsEvent.getShortDescription() != null) {
            formattedShortDesc = new ParameterisedMessage(newsEvent.getShortDescription().toString());
        }
        final NewsEvent.Builder fullMessage = new NewsEvent.Builder(newsEvent.getPlayerId(), formattedMessage)
                .setType(newsEvent.getType())
                .setTitle(newsEvent.getTitle())
                .setShortDescription(formattedShortDesc)
                .setImage(newsEvent.getImage())
                .setDelay(newsEvent.getDelay())
                .setGameType(newsEvent.getGameType());
        addPostedAchievementDetails(newsEvent, fullMessage);
        return JSON_HELPER.serialize(fullMessage.build());
    }

    private Map<String, String> headers() {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(DocumentHeaderType.IS_A_PLAYER.getHeader(), Boolean.toString(true));
        if (tableId != null) {
            headers.put(DocumentHeaderType.TABLE.getHeader(), tableId.toString());
        }
        if (partnerId != null && partnerId.trim().length() > 0) {
            headers.put(DocumentHeaderType.PARTNER.getHeader(), partnerId);
        }
        return headers;
    }

    private static void addPostedAchievementDetails(final NewsEvent event,
                                                    final NewsEvent.Builder fullMessage) {
        if (event.getPostedAchievementTitleText() != null
                && StringUtils.isNotBlank(event.getPostedAchievementTitleText())) {
            fullMessage.setPostedAchievementTitleText(event.getPostedAchievementTitleText());
        } else {
            fullMessage.setPostedAchievementTitleText(DEFAULT_POSTED_ACHIEVEMENT_TEXT);
        }
        if (StringUtils.isNotBlank(event.getPostedAchievementTitleLink())) {
            fullMessage.setPostedAchievementTitleLink(event.getPostedAchievementTitleLink());
        } else {
            fullMessage.setPostedAchievementTitleLink(DEFAULT_POSTED_ACHIEVEMENT_TITLE_LINK);
        }
        if (event.getPostedAchievementActionText() != null
                && StringUtils.isNotBlank(event.getPostedAchievementActionText())) {
            fullMessage.setPostedAchievementActionText(event.getPostedAchievementActionText());
        } else {
            fullMessage.setPostedAchievementActionText(DEFAULT_POSTED_ACHIEVEMENT_TEXT);
        }
        if (event.getPostedAchievementActionLink() != null
                && StringUtils.isNotBlank(event.getPostedAchievementActionLink())) {
            fullMessage.setPostedAchievementActionLink(event.getPostedAchievementActionLink());
        } else {
            fullMessage.setPostedAchievementActionLink(DEFAULT_POSTED_ACHIEVEMENT_TITLE_LINK);
        }
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
        final NewsEventHostDocument rhs = (NewsEventHostDocument) obj;
        return new EqualsBuilder()
                .append(tableId, rhs.tableId)
                .append(partnerId, rhs.partnerId)
                .append(newsEvent, rhs.newsEvent)
                .append(destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tableId)
                .append(partnerId)
                .append(newsEvent)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableId)
                .append(partnerId)
                .append(newsEvent)
                .append(destination)
                .toString();
    }
}
