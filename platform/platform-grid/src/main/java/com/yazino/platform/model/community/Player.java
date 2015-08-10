package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SpaceClass
public class Player implements Serializable {
    private static final long serialVersionUID = 8271843771695879267L;

    private static final Logger LOG = LoggerFactory.getLogger(Player.class);

    private BigDecimal playerId;
    private String name;
    private BigDecimal accountId;
    private String pictureUrl;
    private PaymentPreferences paymentPreferences;
    private Map<BigDecimal, Relationship> relationships;
    private Set<String> tags;
    private DateTime creationTime;
    private DateTime lastPlayed;

    public Player() {
        // for gigaspaces
    }

    public Player(final BigDecimal playerId) {
        // for templates
        this.playerId = playerId;
    }

    public Player(final BigDecimal playerId,
                  final String name,
                  final BigDecimal accountId,
                  final String pictureUrl,
                  final PaymentPreferences paymentPreferences,
                  final DateTime creationTime,
                  final DateTime lastPlayed) {
        this.playerId = playerId;
        this.name = name;
        this.accountId = accountId;
        this.pictureUrl = pictureUrl;
        this.paymentPreferences = paymentPreferences;
        this.creationTime = creationTime;
        this.lastPlayed = lastPlayed;
    }

    @SpaceIndex(type = SpaceIndexType.EXTENDED)
    public String getName() {
        return name;
    }

    @SpaceId
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setRelationship(final BigDecimal withPlayerId,
                                final Relationship relationship) {
        if (relationships == null) {
            relationships = new ConcurrentHashMap<>();
        }
        relationships.put(withPlayerId, relationship);
    }

    public Relationship getRelationshipTo(final BigDecimal targetPlayerId) {
        if (relationships == null) {
            return null;
        }
        return relationships.get(targetPlayerId);
    }

    public Map<BigDecimal, Relationship> getRelationships() {
        return relationships;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setRelationships(final Map<BigDecimal, Relationship> relationships) {
        if (relationships == null) {
            this.relationships = null;
        } else {
            this.relationships = new ConcurrentHashMap<>(relationships);
        }
    }

    @SpaceIndex
    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(final String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public PaymentPreferences getPaymentPreferences() {
        return paymentPreferences;
    }

    public void setPaymentPreferences(final PaymentPreferences paymentPreferences) {
        this.paymentPreferences = paymentPreferences;
    }

    public DateTime getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(final DateTime lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public Map<BigDecimal, Relationship> listRelationships(final RelationshipType... filters) {
        LOG.debug("entering listRelationships for {}", playerId);

        final Map<BigDecimal, Relationship> filteredRelationships = new HashMap<>();

        if (getRelationships() != null) {
            for (final Map.Entry<BigDecimal, Relationship> entry : getRelationships().entrySet()) {
                if (filters == null || filters.length == 0
                        || ArrayUtils.contains(filters, entry.getValue().getType())) {
                    filteredRelationships.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("listRelationships for {} with filter {} found {}.",
                    playerId, ArrayUtils.toString(filters), filteredRelationships);
        }

        return filteredRelationships;
    }

    public Map<BigDecimal, String> retrieveFriends() {
        final Map<BigDecimal, String> friends = new HashMap<>();
        if (relationships != null) {
            for (Map.Entry<BigDecimal, Relationship> entry : relationships.entrySet()) {
                final Relationship relationship = entry.getValue();
                if (relationship != null && relationship.getType().equals(RelationshipType.FRIEND)) {
                    friends.put(entry.getKey(), relationship.getNickname());
                }
            }
        }
        return friends;
    }

    public Map<BigDecimal, Relationship> retrieveRelationships() {
        final Map<BigDecimal, Relationship> result = new HashMap<>();
        if (relationships != null) {
            result.putAll(relationships);
        }
        return result;
    }

    public DateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(final DateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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
        final Player rhs = (Player) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(pictureUrl, rhs.pictureUrl)
                .append(paymentPreferences, rhs.paymentPreferences)
                .append(relationships, rhs.relationships)
                .append(creationTime, rhs.creationTime)
                .append(lastPlayed, rhs.lastPlayed)
                .append(tags, rhs.tags)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(accountId))
                .append(name)
                .append(pictureUrl)
                .append(paymentPreferences)
                .append(relationships)
                .append(creationTime)
                .append(lastPlayed)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(name)
                .append(accountId)
                .append(pictureUrl)
                .append(paymentPreferences)
                .append(relationships)
                .append(creationTime)
                .append(lastPlayed)
                .append(tags)
                .toString();
    }
}
