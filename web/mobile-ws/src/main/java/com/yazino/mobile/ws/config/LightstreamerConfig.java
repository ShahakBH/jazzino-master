package com.yazino.mobile.ws.config;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class LightstreamerConfig {
    private static final int DEFAULT_PORT = 8090;

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public LightstreamerConfig(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public String getHost() {
        return yazinoConfiguration.getString("lightstreamer.host", defaultHost());
    }

    public int getPort() {
        return yazinoConfiguration.getInt("lightstreamer.port", DEFAULT_PORT);
    }

    private static String defaultHost() {
        try {
            return String.format("http://%s", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Failed to configure default lightstreamer url", e);
        }
    }

}
