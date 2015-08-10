package com.yazino.test.game;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameMessageListener {
    private final BigDecimal playerId;

    private final ConcurrentLinkedQueue<GameMessage> queue = new ConcurrentLinkedQueue<GameMessage>();

    public GameMessageListener(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void clear() {
        queue.clear();
    }

    public GameMessage dequeue() {
        GameMessage queuedItem = null;
        while (!queue.isEmpty()) {
            queuedItem = queue.remove();
        }
        return queuedItem;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void receiveDocument(final GameMessage d) {
        queue.add(d);
    }
}
