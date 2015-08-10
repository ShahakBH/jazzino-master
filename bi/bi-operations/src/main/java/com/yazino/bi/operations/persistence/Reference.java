package com.yazino.bi.operations.persistence;

import org.apache.commons.lang3.StringUtils;

public class Reference {
    private String gameId = "";
    private String reference = "";
    private String tableId = "";

    public Reference(final String reference) {
        parseReference(reference);
    }

    public String getTableId() {
        return tableId;
    }

    public String getGameId() {
        return gameId;
    }

    public String getReference() {
        return reference;
    }

    public boolean hasTableId() {
        return StringUtils.isNotBlank(tableId);
    }

    /* Parses the reference field which has the following format: TID|GID or TID|GID|GAMEINFO where
    * TID is the table id, GID is the game id and GAMEINFO is game specific
    * e.g. a highstakes bonus bank related string */
    private void parseReference(final String referenceString) {
        if (StringUtils.isNotBlank(referenceString)) {
            final String[] references = referenceString.split("\\|");
            if (references.length >= 2) {
                tableId = references[0];
                gameId = references[1];
                if (references.length > 2) {
                    reference = references[2];
                }
            } else {
                reference = references[0];
            }
        }
    }
}


