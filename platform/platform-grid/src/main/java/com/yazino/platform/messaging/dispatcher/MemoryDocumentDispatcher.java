package com.yazino.platform.messaging.dispatcher;

import com.google.common.base.Function;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentListener;
import com.yazino.platform.util.ArrayHelper;

import java.math.BigDecimal;
import java.util.*;

public class MemoryDocumentDispatcher implements DocumentDispatcher {
    private Map<BigDecimal, List<DocumentListener>> playerDocumentListeners
            = new HashMap<BigDecimal, List<DocumentListener>>();
    private List<DocumentListener> observerDocumentListeners = new ArrayList<DocumentListener>();
    private Map<BigDecimal, Map<BigDecimal, Document>> lastPlayerAtTableDocument
            = new HashMap<BigDecimal, Map<BigDecimal, Document>>();
    private Map<BigDecimal, Document> lastObserverDocument = new HashMap<BigDecimal, Document>();
    private Document lastDocument;
    private Set<BigDecimal> lastRecipient;
    private long sentCount = 0;

    public void addObserverListener(final DocumentListener dl) {
        observerDocumentListeners.add(dl);
    }

    public void addListener(final BigDecimal playerId,
                            final DocumentListener dl) {
        List<DocumentListener> listeners = playerDocumentListeners.get(playerId);
        if (listeners == null) {
            listeners = new ArrayList<DocumentListener>();
            playerDocumentListeners.put(playerId, listeners);
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

    public void dispatch(final Document document,
                         final Set<BigDecimal> optionalPlayerId) {
        setLastDocument(document);
        setLastRecipient(optionalPlayerId);
        if (document.getHeaders().containsKey(DocumentHeaderType.TABLE.getHeader())) {
            final BigDecimal tableId = new BigDecimal(document.getHeaders().get(DocumentHeaderType.TABLE.getHeader()));
            if (!lastPlayerAtTableDocument.containsKey(tableId)) {
                lastPlayerAtTableDocument.put(tableId, new HashMap<BigDecimal, Document>());
            }
            if (optionalPlayerId == null || optionalPlayerId.size() == 0) {
                lastObserverDocument.put(tableId, document);
                for (DocumentListener listener : observerDocumentListeners) {
                    listener.receiveDocument(document);
                }
            } else {
                for (BigDecimal playerId : optionalPlayerId) {
                    lastPlayerAtTableDocument.get(tableId).put(playerId, document);
                }
            }
        }
        if (optionalPlayerId != null) {
            for (BigDecimal playerId : optionalPlayerId) {
                if (playerDocumentListeners.containsKey(playerId)) {
                    for (DocumentListener listener : playerDocumentListeners.get(playerId)) {
                        listener.receiveDocument(document);
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

    public <T> void dispatch(final Document document,
                             final Function<T, BigDecimal> converter,
                             final T... players) {
        setLastDocument(document);
        final BigDecimal[] rec = ArrayHelper.convert(players, new BigDecimal[players.length], converter);
        setLastRecipient(new HashSet<BigDecimal>(Arrays.asList(rec)));
        setSentCount(getSentCount() + 1);
    }

    public Document getLastPlayerDocument(final BigDecimal tableId,
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
        playerDocumentListeners.clear();
        observerDocumentListeners.clear();
    }

}
