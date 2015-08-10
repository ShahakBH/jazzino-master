package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GamesConfig {

    private static final String DEFAULT_CMD_PATH = "/game-server/command";

    private String mCommandHost = defaultHost();
    private String mCommandPath = DEFAULT_CMD_PATH;
    private String mDefaultHost = defaultHost();

    public String getCommandPath() {
        return mCommandPath;
    }

    public void setCommandPath(String commandPath) {
        Validate.notNull(commandPath);
        mCommandPath = commandPath;
    }

    public String getCommandHost() {
        return mCommandHost;
    }

    public void setCommandHost(final String commandHost) {
        Validate.notNull(commandHost);
        mCommandHost = commandHost;
    }

    public String getDefaultHost() {
        return mDefaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        Validate.notNull(defaultHost);
        mDefaultHost = defaultHost;
    }

    private static String defaultHost() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return String.format("http://%s", hostName);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Failed to configure default command url", e);
        }
    }
}
