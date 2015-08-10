package com.yazino.platform.messaging.host.format;

import com.yazino.platform.messaging.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.yazino.game.api.ParameterisedMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class HostDocumentDeserialiser {

    private final String documentBody;

    public HostDocumentDeserialiser(final String documentBody) {
        this.documentBody = documentBody;
    }

    public HostDocumentDeserialiser(final Document document) {
        notNull(document, "document may not be null");

        this.documentBody = document.getBody();
    }

    public Map<String, Object> body() {
        try {
            final Map<String, Object> out = new HashMap<String, Object>();
            final JSONObject jsonObject = new JSONObject(documentBody);
            final String gameId = jsonObject.getString(HostDocumentHeader.GAME_ID.getHeader());
            out.put(HostDocumentHeader.GAME_ID.getHeader(), Long.parseLong(gameId));
            final JSONObject messageJson = jsonObject.optJSONObject(HostDocumentHeader.MESSAGE.getHeader());
            if (messageJson != null) {
                out.put(HostDocumentHeader.MESSAGE.getHeader(), deserialiseMessage(messageJson));
            }
            final String changes = jsonObject.optString(HostDocumentHeader.CHANGES.getHeader());
            out.put(HostDocumentHeader.CHANGES.getHeader(), changes);
            final JSONObject tableProperties = jsonObject.optJSONObject(
                    HostDocumentHeader.TABLE_PROPERTIES.getHeader());
            if (tableProperties != null) {
                out.put(HostDocumentHeader.TABLE_PROPERTIES.getHeader(), deserializeProperties(tableProperties));
            }
            final String commandUUID = jsonObject.getString(HostDocumentHeader.COMMAND_UUID.getHeader());
            out.put(HostDocumentHeader.COMMAND_UUID.getHeader(), commandUUID);
            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    private Map<String, String> deserializeProperties(final JSONObject properties)
            throws JSONException {
        final Map<String, String> tableProperties = new HashMap<String, String>();
        final Iterator iterator = properties.keys();
        while (iterator.hasNext()) {
            final String key = iterator.next().toString();
            tableProperties.put(key, properties.getString(key));
        }
        return tableProperties;
    }

    private ParameterisedMessage deserialiseMessage(final JSONObject jsonObject) throws JSONException {
        final JSONArray args = jsonObject.getJSONArray("parameters");
        final Object[] argArr = new Object[args.length()];
        for (int i = 0; i < args.length(); i++) {
            argArr[i] = args.get(i);
        }
        return new ParameterisedMessage(jsonObject.getString(HostDocumentHeader.MESSAGE.getHeader()), argArr);
    }
}
