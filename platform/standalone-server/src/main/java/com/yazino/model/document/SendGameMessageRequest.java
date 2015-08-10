package com.yazino.model.document;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class SendGameMessageRequest {

    private String body;
    private String type;
    private String playerIds;
    private boolean tableMessage;

    public boolean isTableMessage() {
        return tableMessage;
    }

    public void setTableMessage(final boolean tableMessage) {
        this.tableMessage = tableMessage;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(final String playerIds) {
        this.playerIds = playerIds;
    }

    public Set<BigDecimal> convertPlayerIds() {
        final String[] idsAsString = playerIds.split(",");
        final Set<BigDecimal> result = new HashSet<BigDecimal>();
        for (String idAsString : idsAsString) {
            result.add(new BigDecimal(idAsString.trim()));
        }
        return result;
    }
}
