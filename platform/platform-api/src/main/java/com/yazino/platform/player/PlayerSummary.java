package com.yazino.platform.player;

import com.google.common.base.Optional;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerSummary implements Serializable {
    private static final long serialVersionUID = -6534998648821392205L;

    private final Map<String, BigDecimal> purchasesByCurrency = new HashMap<>();
    private final Map<String, Integer> levelsByGame = new HashMap<>();
    private final Set<String> tags = new HashSet<>();

    private final BigDecimal playerId;
    private final BigDecimal accountId;
    private final String avatarUrl;
    private final DateTime lastPlayed;
    private final DateTime registration;
    private final BigDecimal balance;
    private final String realName;
    private final String displayName;
    private final String emailAddress;
    private final String providerName;
    private final String externalId;
    private final String countryCode;
    private final Gender gender;
    private final PlayerProfileStatus status;
    private final PlayerProfileRole role;
    private final BigDecimal purchasedChips;

    public PlayerSummary(final BigDecimal playerId,
                         final BigDecimal accountId,
                         final String avatarUrl,
                         final DateTime lastPlayed,
                         final DateTime registration,
                         final BigDecimal balance,
                         final String realName,
                         final String displayName,
                         final String emailAddress,
                         final String providerName,
                         final String externalId,
                         final String countryCode,
                         final Gender gender,
                         final PlayerProfileStatus status,
                         final PlayerProfileRole role,
                         final BigDecimal purchasedChips,
                         final Map<String, BigDecimal> purchasesByCurrency,
                         final Map<String, Integer> levelsByGame,
                         final Set<String> tags) {
        notNull(playerId, "playerId may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(registration, "registration may not be null");
        notNull(balance, "balance may not be null");
        notNull(providerName, "providerName may not be null");
        notNull(purchasedChips, "purchasedChips may not be null");
        notNull(status, "status may not be null");
        notNull(role, "role may not be null");

        this.playerId = playerId;
        this.accountId = accountId;
        this.avatarUrl = avatarUrl;
        this.lastPlayed = lastPlayed;
        this.registration = registration;
        this.balance = balance;
        this.realName = realName;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.providerName = providerName;
        this.externalId = externalId;
        this.countryCode = countryCode;
        this.gender = gender;
        this.status = status;
        this.role = role;
        this.purchasedChips = purchasedChips;

        if (purchasesByCurrency != null) {
            this.purchasesByCurrency.putAll(purchasesByCurrency);
        }
        if (levelsByGame != null) {
            this.levelsByGame.putAll(levelsByGame);
        }
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public Map<String, BigDecimal> getPurchasesByCurrency() {
        return Collections.unmodifiableMap(purchasesByCurrency);
    }

    public Map<String, Integer> getLevelsByGame() {
        return Collections.unmodifiableMap(levelsByGame);
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public Optional<String> getAvatarUrl() {
        return Optional.fromNullable(avatarUrl);
    }

    public Optional<DateTime> getLastPlayed() {
        return Optional.fromNullable(lastPlayed);
    }

    public DateTime getRegistration() {
        return registration;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Optional<String> getRealName() {
        return Optional.fromNullable(realName);
    }

    public Optional<String> getDisplayName() {
        return Optional.fromNullable(displayName);
    }

    public Optional<String> getEmailAddress() {
        return Optional.fromNullable(emailAddress);
    }

    public String getProviderName() {
        return providerName;
    }

    public Optional<String> getExternalId() {
        return Optional.fromNullable(externalId);
    }

    public Optional<String> getCountryCode() {
        return Optional.fromNullable(countryCode);
    }

    public Optional<Gender> getGender() {
        return Optional.fromNullable(gender);
    }

    public PlayerProfileStatus getStatus() {
        return status;
    }

    public BigDecimal getPurchasedChips() {
        return purchasedChips;
    }

    public PlayerProfileRole getRole() {
        return role;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
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
        final PlayerSummary rhs = (PlayerSummary) obj;
        return new EqualsBuilder()
                .append(avatarUrl, rhs.avatarUrl)
                .append(lastPlayed, rhs.lastPlayed)
                .append(registration, rhs.registration)
                .append(balance, rhs.balance)
                .append(realName, rhs.realName)
                .append(displayName, rhs.displayName)
                .append(emailAddress, rhs.emailAddress)
                .append(providerName, rhs.providerName)
                .append(externalId, rhs.externalId)
                .append(countryCode, rhs.countryCode)
                .append(gender, rhs.gender)
                .append(status, rhs.status)
                .append(role, rhs.role)
                .append(purchasedChips, rhs.purchasedChips)
                .append(purchasesByCurrency, rhs.purchasesByCurrency)
                .append(levelsByGame, rhs.levelsByGame)
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
                .append(avatarUrl)
                .append(lastPlayed)
                .append(registration)
                .append(balance)
                .append(realName)
                .append(displayName)
                .append(emailAddress)
                .append(providerName)
                .append(externalId)
                .append(countryCode)
                .append(gender)
                .append(status)
                .append(role)
                .append(purchasedChips)
                .append(purchasesByCurrency)
                .append(levelsByGame)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(accountId)
                .append(avatarUrl)
                .append(lastPlayed)
                .append(registration)
                .append(balance)
                .append(realName)
                .append(displayName)
                .append(emailAddress)
                .append(providerName)
                .append(externalId)
                .append(countryCode)
                .append(gender)
                .append(status)
                .append(role)
                .append(purchasedChips)
                .append(purchasesByCurrency)
                .append(levelsByGame)
                .append(tags)
                .toString();
    }
}
