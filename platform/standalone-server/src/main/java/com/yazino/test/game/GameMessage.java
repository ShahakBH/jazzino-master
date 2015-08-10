package com.yazino.test.game;

import com.yazino.platform.messaging.host.format.HostDocumentDeserialiser;

import java.util.Map;

public class GameMessage {

    public enum Type {
        ERROR,
        GAME_STATUS,
        INITIAL_GAME_STATUS
    }

    private Type type;
    private String body;

    public GameMessage(final String type,
                       final String body) {
        this.type = Type.valueOf(type);
        this.body = body;
    }

    public Type getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public String getChanges() {
        final Map<String, Object> documentBody = new HostDocumentDeserialiser(body).body();
        final Object result = documentBody.get("changes");
        return result.toString();
    }

    public Object getBodyMessage() {
        final Map<String, Object> documentBody = new HostDocumentDeserialiser(body).body();
        return documentBody.get("message");
    }
}
