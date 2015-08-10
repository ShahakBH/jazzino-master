package com.yazino.web.session;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public final class LobbySessionReference implements Serializable {
    private static final long serialVersionUID = -5298480602945873341L;

    private static final Logger LOG = LoggerFactory.getLogger(LobbySessionReference.class);

    private static final String COOKIE_DELIMITER = "+";

    private final BigDecimal playerId;
    private final String sessionKey;
    private final Platform platform;
    private final AuthProvider authProvider;

    private LobbySessionReference(final BigDecimal playerId,
                                  final String sessionKey,
                                  final Platform platform,
                                  final AuthProvider authProvider) {
        this.playerId = playerId;
        this.sessionKey = sessionKey;
        this.platform = platform;
        this.authProvider = authProvider;
    }

    public LobbySessionReference(final LobbySession lobbySession) {
        notNull(lobbySession, "lobbySession may not be null");

        this.playerId = lobbySession.getPlayerId();
        this.sessionKey = lobbySession.getLocalSessionKey();
        this.platform = lobbySession.getPlatform();
        this.authProvider = lobbySession.getAuthProvider();
    }

    @SuppressWarnings("deprecation") // required for compatibility with old accounts
    public static LobbySessionReference fromEncodedSession(final String encodedSession) {
        final String packedValue = decode(encodedSession);
        if (packedValue == null) {
            LOG.debug("Session Request '{}' has a null encoded value.", encodedSession);
            return null;
        }
        if (!packedValue.contains(COOKIE_DELIMITER)) {
            LOG.debug("Session Request '{}' is missing delimiter '{}'", packedValue, COOKIE_DELIMITER);
            return null;
        }

        final String[] split = packedValue.split("\\" + COOKIE_DELIMITER);
        if (split.length > 4) {
            LOG.debug("Session Request '{}' is badly formed.", packedValue);
            return null;
        }

        try {
            Platform platform = Platform.WEB;
            if (split.length > 2) {
                platform = Platform.valueOf(split[2]);
            }

            AuthProvider providerName = AuthProvider.YAZINO;
            if (split.length > 3) {
                providerName = AuthProvider.parseProviderName(split[3]);
            }

            LOG.debug("Session Request is {}:{}:{}", split[0], split[1], platform);
            return new LobbySessionReference(new BigDecimal(split[0]), split[1], platform, providerName);

        } catch (Exception e) {
            LOG.debug("Session Request query resulted in an error", e);
            return null;
        }
    }

    public String encode() {
        Platform encodingPlatform = platform;
        if (platform == null) {
            LOG.warn("Platform is null. Assuming WEB");
            encodingPlatform = Platform.WEB;
        }
        return encode(playerId + COOKIE_DELIMITER + sessionKey + COOKIE_DELIMITER + encodingPlatform + COOKIE_DELIMITER + authProvider);
    }

    private String encode(final String toEncode) {
        LOG.debug("Encoding {}", toEncode);
        try {
            return new String(Base64.encodeBase64(toEncode.getBytes("UTF-8"), false), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to encode", e);
            return null;
        }
    }

    private static String decode(final String toDecode) {
        LOG.debug("Decoding {}", toDecode);
        try {
            return new String(Base64.decodeBase64(toDecode.getBytes("UTF-8")), "UTF-8");
        } catch (Exception e) {
            LOG.error("Invalid string sent to decoder " + toDecode);
            return null;
        }
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public Platform getPlatform() {
        return platform;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
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
        final LobbySessionReference rhs = (LobbySessionReference) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(sessionKey, rhs.sessionKey)
                .append(platform, rhs.platform)
                .append(authProvider, rhs.authProvider)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(sessionKey)
                .append(platform)
                .append(authProvider)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(sessionKey)
                .append(platform)
                .append(authProvider)
                .toString();
    }
}
