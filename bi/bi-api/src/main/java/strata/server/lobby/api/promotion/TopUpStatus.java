package strata.server.lobby.api.promotion;

public enum TopUpStatus {
    /**
     * player has been topped  AND the client has acknowledged the top up result
     * (i.e. for web: the popup has been displayed)
     */
    ACKNOWLEDGED,

    /**
     * player has been topped, the client has NOT acknowledged the top up result
     * (i.e for web, the popup has not been displayed)
     */
    CREDITED,

    /**
     * player has no recorded top up, should only happen for new players, should request info again
     */
    NEVER_CREDITED
}
