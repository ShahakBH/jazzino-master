package com.yazino.bi.opengraph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

public class AccessTokenStore {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenStore.class);

    private LruMap<Key, AccessToken> accessTokensByKey;


    public AccessTokenStore(int maxCapacity) {
        accessTokensByKey = new LruMap<>(maxCapacity);
    }

    public void storeAccessToken(final Key key, final AccessToken token) {
        LOG.debug("Storing access token {} for key {}", token.getAccessToken(), key);
        accessTokensByKey.put(key, token);
    }

    public AccessToken findByKey(final Key key) {
        return accessTokensByKey.get(key);
    }

    public void invalidateToken(final Key key) {
        LOG.debug("Invalidating access token for key {}", key);
        accessTokensByKey.remove(key);
    }

    public Map<Key, AccessToken> getAccessTokensByKey() {
        return Collections.unmodifiableMap(this.accessTokensByKey);
    }

    public static class Key {

        private final String gameType;
        private final BigInteger playerId;

        public Key(final BigInteger playerId, final String gameType) {
            this.gameType = gameType;
            this.playerId = playerId;
        }

        public String getGameType() {
            return gameType;
        }

        public BigInteger getPlayerId() {
            return playerId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key that = (Key) o;
            return new EqualsBuilder()
                    .append(gameType, that.gameType)
                    .append(playerId, that.playerId)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(351, 9871)
                    .append(gameType)
                    .append(playerId)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this).toString();
        }
    }

    public static class AccessToken {
        private final String accessToken;

        public AccessToken(final String accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this).toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final AccessToken that = (AccessToken) o;
            return new EqualsBuilder()
                    .append(accessToken, that.accessToken)
                    .isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(351, 9871)
                    .append(accessToken)
                    .toHashCode();
        }
    }
}
