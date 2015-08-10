package com.yazino.web.payment;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

public class ProductRequestContext {
    private Platform platform;
    private BigDecimal playerId;
    private String gameType;
    private HttpServletRequest request;

    public ProductRequestContext(Platform platform, BigDecimal playerId, String gameType, HttpServletRequest request) {
        this.platform = platform;
        this.playerId = playerId;
        this.gameType = gameType;
        this.request = request;
    }

    public Platform getPlatform() {
        return platform;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public HttpServletRequest getRequest() {
        return request;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("platform", platform)
                .append("playerId", playerId)
                .append("gameType", gameType)
                .append("request", request)
                .toString();
    }
}
