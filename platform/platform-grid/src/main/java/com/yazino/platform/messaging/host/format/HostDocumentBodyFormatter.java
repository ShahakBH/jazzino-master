package com.yazino.platform.messaging.host.format;

import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import com.yazino.game.api.ObservableChange;
import com.yazino.game.api.ObservableTimeOutEventInfo;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HostDocumentBodyFormatter {

    private final Map<HostDocumentHeader, String> contents = new HashMap<HostDocumentHeader, String>();

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final long gameId;
    private final String commandUUID;

    public static HostDocumentBodyFormatter body(final long gameId,
                                                 final String commandUUID) {
        return new HostDocumentBodyFormatter(gameId, commandUUID);
    }

    public HostDocumentBodyFormatter(final long gameId,
                                     final String commandUUID) {
        this.gameId = gameId;
        this.commandUUID = commandUUID;
        if (contents != null) {
            this.contents.putAll(contents);
        }
    }

    public String build() {
        final StringBuilder sb = new StringBuilder("{");
        HostDocumentHeader.GAME_ID.appendTo(sb, gameId);
        sb.append(", ");
        HostDocumentHeader.COMMAND_UUID.appendTo(sb, commandUUID);
        for (Map.Entry<HostDocumentHeader, String> entry : contents.entrySet()) {
            sb.append(", ");
            entry.getKey().appendTo(sb, entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    public HostDocumentBodyFormatter withWarningCodes(final Set<String> warningCodes) {
        final String warningText;
        if (warningCodes == null || warningCodes.isEmpty()) {
            warningText = "";
        } else {
            warningText = StringUtils.join(warningCodes, "\\t");
        }
        contents.put(HostDocumentHeader.WARNINGS, warningText);
        return this;
    }

    public HostDocumentBodyFormatter withPlayerStatus(final boolean aPlayer) {
        contents.put(HostDocumentHeader.IS_A_PLAYER, Boolean.toString(aPlayer));
        return this;
    }

    public HostDocumentBodyFormatter withTableProperties(final Map<String, String> tableProperties) {
        contents.put(HostDocumentHeader.TABLE_PROPERTIES, JSON_HELPER.serialize(tableProperties));
        return this;
    }

    public HostDocumentBodyFormatter withMessage(final ParameterisedMessage message) {
        contents.put(HostDocumentHeader.MESSAGE, JSON_HELPER.serialize(message));
        return this;
    }

    public HostDocumentBodyFormatter withChanges(final ObservableDocumentContext context) {
        contents.put(HostDocumentHeader.CHANGES, buildChanges(context));
        return this;
    }

    private String buildChanges(final ObservableDocumentContext context) {
        final StringBuilder changes = new StringBuilder();
        //game id
        changes.append(context.getGameId());
        changes.append("\\t");
        changes.append(context.getIncrementOfGameStart());
        //tab delimited player ids
        for (BigDecimal playerId : context.getPlayerIds()) {
            changes.append("\\t").append(playerId.toString());
        }
        //player balance
        changes.append("\\n");
        if (context.getPlayerBalance() != null) {
            changes.append(context.getPlayerBalance());
        } else {
            changes.append("NA");
        }
        //current increment
        changes.append("\\n");
        if (context.getStatus() != null) {
            changes.append(context.getIncrement());
        } else {
            changes.append("NA");
        }
        //allowed actions
        changes.append("\\n");
        if (context.getStatus() != null) {
            appendDelimitedString(changes, context.getStatus().getAllowedActions(), "\\t");
        }
        //next timeout
        changes.append("\\n");
        final ObservableTimeOutEventInfo nextEvent;
        if (context.getStatus() == null) {
            nextEvent = null;
        } else {
            nextEvent = context.getStatus().getNextEvent();
        }
        if (nextEvent != null) {
            changes.append(nextEvent.getType());
            changes.append("\\t");
            changes.append(nextEvent.getInitalMillisTillTimeout());
            changes.append("\\t");
            changes.append(nextEvent.getMillisTillEvent());
        } else {
            changes.append("NoTimeout");
        }
        //changes
        for (ObservableChange change : context.getMergedGameChanges()) {
            changes.append("\\n");
            changes.append(change.getIncrement());
            changes.append("\\t");
            appendDelimitedString(changes, change.getArgs(), "\\t");
        }
        return changes.toString();
    }

    private <T> void appendDelimitedString(final StringBuilder sb,
                                           final Collection<T> items,
                                           final String delimiter) {
        boolean comma = false;
        for (T item : items) {
            if (comma) {
                sb.append(delimiter);
            } else {
                comma = true;
            }
            sb.append(item);
        }
    }

    private <T> void appendDelimitedString(final StringBuilder sb,
                                           final T[] items,
                                           final String delimiter) {
        boolean comma = false;
        for (T item : items) {
            if (comma) {
                sb.append(delimiter);
            } else {
                comma = true;
            }
            sb.append(item);
        }
    }
}
