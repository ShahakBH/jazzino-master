package com.yazino.bi.operations.model;

import java.math.BigDecimal;

/**
 * Data common to all player information structures
 */
public interface PlayerData {
    /**
     * .
     *
     * @return User's unique ID
     */
    BigDecimal getPlayerProfileId();

    /**
     * Sets the user's unique ID
     *
     * @param playerProfileId .
     */
    void setPlayerProfileId(BigDecimal playerProfileId);

    /**
     * .
     *
     * @return Player's unique ID
     */
    BigDecimal getPlayerId();

    /**
     * Sets the player's unique ID
     *
     * @param playerId .
     */
    void setPlayerId(BigDecimal playerId);

    /**
     * .
     *
     * @return User's real name
     */
    String getUserName();

    /**
     * Sets the player's real name
     *
     * @param playerName .
     */
    void setUserName(String playerName);

    /**
     * .
     *
     * @return User's display name
     */
    String getDisplayName();

    /**
     * Sets the player's display name
     *
     * @param playerName .
     */
    void setDisplayName(String playerName);

    /**
     * .
     *
     * @return User's email address
     */
    String getEmailAddress();

    /**
     * Sets the player's email address
     *
     * @param playerName .
     */
    void setEmailAddress(String playerName);

    /**
     * .
     *
     * @return User's picture location
     */
    String getPictureLocation();

    /**
     * Sets the player's picture location
     *
     * @param playerName .
     */
    void setPictureLocation(String playerName);

    String getProvider();

    void setProvider(String provider);

    boolean isVerified();

    void setVerified(boolean verified);
}
