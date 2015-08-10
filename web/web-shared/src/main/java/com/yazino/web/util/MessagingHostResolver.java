package com.yazino.web.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("messagingHostResolver")
public class MessagingHostResolver {

    private Map<Integer, String> messagingServers = new HashMap<Integer, String>();

    @Autowired
    public MessagingHostResolver(@Value("${strata.rabbitmq.host}") final String messagingHost) {
        notBlank(messagingHost, "messagingHost not present");
        final String[] servers = messagingHost.split(",");
        int partition = 0;
        for (String server : servers) {
            messagingServers.put(partition, server.trim());
            partition++;
        }
    }

    public String resolveMessagingHostForPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId is required");
        final int hostIndex = playerId.intValue() % messagingServers.size();
        return messagingServers.get(hostIndex);
    }
}
