package com.yazino.platform.messaging;

public interface RabbitMQRoutingKeyWorker {
    String getRoutingKey(String documentType, String playerId, String tableId);
}
