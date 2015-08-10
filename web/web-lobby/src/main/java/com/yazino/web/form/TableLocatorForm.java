package com.yazino.web.form;

import static org.tuckey.web.filters.urlrewrite.utils.StringUtils.isBlank;

public class TableLocatorForm {
    public static final String GAME_TYPE_BLANK = "Game type is blank";

    private String gameType;
    private String clientId;
    private String variationName;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getVariationName() {
        return variationName;
    }

    public void setVariationName(final String variationName) {
        this.variationName = variationName;
    }

    public String validate() {
        if (isBlank(gameType)) {
            return GAME_TYPE_BLANK;
        }
        return null;
    }
}
