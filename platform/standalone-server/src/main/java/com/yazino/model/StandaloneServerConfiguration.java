package com.yazino.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StandaloneServerConfiguration {
    @Autowired
    @Value("${standalone-server.rabbitmq.host}")
    private String rabbitHost;

    @Autowired
    @Value("${standalone-server.rabbitmq.virtualHost}")
    private String rabbitVirtualHost;

    @Autowired
    @Value("${standalone-server.flash-assets.loaderSwf}")
    private String loaderSwf;

    @Autowired
    @Value("${standalone-server.flash-assets.gameSwf}")
    private String gameSwf;

    @Autowired
    @Value("${standalone-server.game-type}")
    private String gameType;

    @Autowired
    @Value("${standalone-server.game-rules.class}")
    private String gameRulesClass;

    public String getRabbitHost() {
        return rabbitHost;
    }

    public String getRabbitVirtualHost() {
        return rabbitVirtualHost;
    }

    public String getGameSwf() {
        return gameSwf;
    }

    public void setRabbitHost(final String rabbitHost) {
        this.rabbitHost = rabbitHost;
    }

    public void setRabbitVirtualHost(final String rabbitVirtualHost) {
        this.rabbitVirtualHost = rabbitVirtualHost;
    }

    public void setGameSwf(final String gameSwf) {
        this.gameSwf = gameSwf;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getGameRulesClassName() {
        return gameRulesClass;
    }

    public void setGameRulesClass(final String gameRulesClass) {
        this.gameRulesClass = gameRulesClass;
    }

    public String getLoaderSwf() {
        return loaderSwf;
    }

    public void setLoaderSwf(final String loaderSwf) {
        this.loaderSwf = loaderSwf;
    }
}
