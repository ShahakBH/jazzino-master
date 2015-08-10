package com.yazino.web.domain;

import com.yazino.platform.Partner;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("launchConfiguration")
public class LaunchConfiguration implements Serializable {
    private static final long serialVersionUID = 1274988774670121445L;

    private final String messagingPort;
    private final String messagingHost;
    private final String messagingVirtualHost;
    private final String contentUrl;
    private final String permanentContentUrl;
    private final String clientUrl;
    private final String commandBaseUrl;
    private final Partner partnerId;

    @Autowired
    public LaunchConfiguration(@Value("${strata.rabbitmq.host}") final String messagingHost,
                               @Value("${strata.rabbitmq.virtualhost}") final String messagingVirtualHost,
                               @Value("${strata.rabbitmq.port}") final String messagingPort,
                               @Value("${senet.web.content}") final String contentUrl,
                               @Value("${senet.web.application-content}") final String clientUrl,
                               @Value("${senet.web.command}") final String commandBaseUrl,
                               @Value("${strata.lobby.partnerid}") final Partner partnerId,
                               @Value("${senet.web.permanent-content}") final String permanentContentUrl) {
        this.messagingPort = messagingPort;
        this.messagingHost = messagingHost;
        this.messagingVirtualHost = messagingVirtualHost;
        this.contentUrl = appendTrailingSlash(contentUrl);
        this.clientUrl = appendTrailingSlash(clientUrl);
        this.commandBaseUrl = appendTrailingSlash(commandBaseUrl);
        this.partnerId = partnerId;
        this.permanentContentUrl = appendTrailingSlash(permanentContentUrl);
    }

    private String appendTrailingSlash(final String url) {
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    public String getMessagingPort() {
        return messagingPort;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getCommandBaseUrl() {
        return commandBaseUrl;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public String getMessagingVirtualHost() {
        return messagingVirtualHost;
    }

    public String getPermanentContentUrl() {
        return permanentContentUrl;
    }

    public String getMessagingHost() {
        return messagingHost;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final LaunchConfiguration rhs = (LaunchConfiguration) obj;
        return new EqualsBuilder()
                .append(messagingVirtualHost, rhs.messagingVirtualHost)
                .append(messagingHost, rhs.messagingHost)
                .append(messagingPort, rhs.messagingPort)
                .append(contentUrl, rhs.contentUrl)
                .append(clientUrl, rhs.clientUrl)
                .append(commandBaseUrl, rhs.commandBaseUrl)
                .append(partnerId, rhs.partnerId)
                .append(permanentContentUrl, rhs.permanentContentUrl)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(messagingVirtualHost)
                .append(messagingHost)
                .append(messagingPort)
                .append(contentUrl)
                .append(clientUrl)
                .append(permanentContentUrl)
                .append(commandBaseUrl)
                .append(partnerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
