package com.yazino.platform.test.game.status;

import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;
import com.yazino.game.generic.player.Player;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.yazino.game.api.document.DocumentAccessors.documentFor;
import static org.apache.commons.lang3.Validate.notNull;

public class GamePlayerStatus implements Serializable, Player, Documentable {
    private static final long serialVersionUID = -5271901984355200298L;

    private final GamePlayer player;
    private final Set<String> transactionReferences;

    public GamePlayerStatus(GamePlayer player) {
        this(player, new HashSet<String>());
    }

    private GamePlayerStatus(GamePlayer player, Set<String> transactionReferences) {
        this.player = player;
        this.transactionReferences = transactionReferences;
    }

    @SuppressWarnings("unchecked")
    public GamePlayerStatus(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        player = GamePlayer.fromDocument(documentFor(document, "player"));
        transactionReferences = (Set<String>) document.get("transactionReferences");
    }

    public static GamePlayerStatus fromDocument(final Map<String, Object> document) {
        if (document == null || document.isEmpty()) {
            return null;
        }
        return new GamePlayerStatus(document);
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder()
                .withDocument("player", player)
                .withCollectionOfString("transactionReferences", transactionReferences)
                .toDocument();
    }

    @Override
    public GamePlayer getPlayer() {
        return player;
    }

    @Override
    public boolean hasPendingTransaction() {
        return !transactionReferences.isEmpty();
    }

    public boolean hasTransactionReference(String reference) {
        return transactionReferences.contains(reference);
    }

    public GamePlayerStatus removeReference(String reference) {
        if (transactionReferences.contains(reference)) {
            transactionReferences.remove(reference);
        }
        return new GamePlayerStatus(player, transactionReferences);
    }
}
