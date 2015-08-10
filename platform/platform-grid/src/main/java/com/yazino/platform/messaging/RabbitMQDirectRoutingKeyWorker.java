package com.yazino.platform.messaging;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RabbitMQDirectRoutingKeyWorker implements RabbitMQRoutingKeyWorker {

    private static final String DELIMITER = ".";

    public String getRoutingKey(final String documentType,
                                final String playerId,
                                final String tableId) {
        final StringBuilder part1 = new StringBuilder();
        final StringBuilder part2 = new StringBuilder();

        if (!isBlank(playerId)) {
            part1.append("PLAYER");
            part2.append(DELIMITER).append(playerId);
        }
        if (!isBlank(tableId)) {
            part1.append("TABLE");
            part2.append(DELIMITER).append(tableId);
        }
        return part1.append(part2).toString();
    }

}
