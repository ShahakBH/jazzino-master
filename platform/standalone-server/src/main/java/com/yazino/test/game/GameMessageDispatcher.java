package com.yazino.test.game;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;

import java.math.BigDecimal;
import java.util.*;

public class GameMessageDispatcher implements DocumentDispatcher {

    private Map<BigDecimal, List<GameMessageListener>> playerGameMessageListeners
            = new HashMap<>();
    private List<GameMessageListener> observerGameMessageListeners = new ArrayList<>();
    private Map<BigDecimal, Map<BigDecimal, GameMessage>> lastPlayerAtTableDocument
            = new HashMap<>();
    private Map<BigDecimal, Document> lastObserverDocument = new HashMap<>();
    private Document lastDocument;
    private Set<BigDecimal> lastRecipient;
    private long sentCount = 0;

    public void addObserverListener(final GameMessageListener dl) {
        observerGameMessageListeners.add(dl);
    }

    public void addListener(final BigDecimal playerId,
                            final GameMessageListener dl) {
        List<GameMessageListener> listeners = playerGameMessageListeners.get(playerId);
        if (listeners == null) {
            listeners = new ArrayList<>();
            playerGameMessageListeners.put(playerId, listeners);
        }
        listeners.add(dl);
    }

    public void setLastDocument(final Document lastDocument) {
        this.lastDocument = lastDocument;
    }

    public Set<BigDecimal> getLastRecipient() {
        return lastRecipient;
    }

    public void setLastRecipient(final Set<BigDecimal> lastRecipient) {
        this.lastRecipient = lastRecipient;
    }

    public long getSentCount() {
        return sentCount;
    }

    public void setSentCount(final long sentCount) {
        this.sentCount = sentCount;
    }

    public void dispatch(final Document document) {
        dispatch(document, new HashSet<BigDecimal>());
    }

    public void dispatch(final Document document,
                         final BigDecimal playerId) {
        final Set<BigDecimal> playerIds = new HashSet<BigDecimal>();
        playerIds.add(playerId);
        dispatch(document, playerIds);
    }

    private GameMessage convertDocumentToGameMessage(final Document document) {
        return new GameMessage(document.getType(), document.getBody());
    }

    public void dispatch(final Document document,
                         final Set<BigDecimal> optionalPlayerId) {
        setLastDocument(document);
        setLastRecipient(optionalPlayerId);
        final GameMessage message = convertDocumentToGameMessage(document);
        if (document.getHeaders().containsKey(DocumentHeaderType.TABLE.getHeader())) {
            final BigDecimal tableId = new BigDecimal(document.getHeaders().get(DocumentHeaderType.TABLE.getHeader()));
            if (!lastPlayerAtTableDocument.containsKey(tableId)) {
                lastPlayerAtTableDocument.put(tableId, new HashMap<BigDecimal, GameMessage>());
            }
            if (optionalPlayerId == null || optionalPlayerId.size() == 0) {
                lastObserverDocument.put(tableId, document);
                for (GameMessageListener listener : observerGameMessageListeners) {
                    listener.receiveDocument(message);
                }
            } else {
                for (BigDecimal playerId : optionalPlayerId) {
                    lastPlayerAtTableDocument.get(tableId).put(playerId, message);
                }
            }
        }
        if (optionalPlayerId != null) {
            for (BigDecimal playerId : optionalPlayerId) {
                if (playerGameMessageListeners.containsKey(playerId)) {
                    for (GameMessageListener listener : playerGameMessageListeners.get(playerId)) {
                        listener.receiveDocument(message);
                    }
                }
            }
        }
        if (optionalPlayerId == null) {
            setSentCount(getSentCount() + 1);
        } else {
            setSentCount(getSentCount() + optionalPlayerId.size());
        }
    }

    public GameMessage getLastPlayerDocument(final BigDecimal tableId,
                                             final BigDecimal playerId) {
        if (!lastPlayerAtTableDocument.containsKey(tableId)) {
            return null;
        }
        if (!lastPlayerAtTableDocument.get(tableId).containsKey(playerId)) {
            return null;
        }
        return lastPlayerAtTableDocument.get(tableId).get(playerId);
    }

    public Document getLastDocument() {
        return lastDocument;
    }

    public Document getLastObserverDocument(final BigDecimal tableId) {
        if (!lastObserverDocument.containsKey(tableId)) {
            return null;
        }
        return lastObserverDocument.get(tableId);
    }

    public void clear() {
        setLastDocument(null);
        setLastRecipient(null);
        lastPlayerAtTableDocument.clear();
        lastObserverDocument.clear();
        setSentCount(0);
    }

    public void clearListeners() {
        playerGameMessageListeners.clear();
        observerGameMessageListeners.clear();
    }

}
