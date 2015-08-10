package com.yazino.platform.model.chat;


import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class ChatChannelStatusHostDocument implements HostDocument {
    private static final long serialVersionUID = -1499420559624526684L;

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final ChatChannelAggregate chatChannelAggregate;
    private final Destination destination;

    public ChatChannelStatusHostDocument(final ChatChannelAggregate chatChannelAggregate,
                                         final Destination destination) {
        notNull(chatChannelAggregate, "chatChannelAggregate may not be null");
        notNull(destination, "destination may not be null");

        this.chatChannelAggregate = chatChannelAggregate;
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        final Document document = new Document(DocumentType.CHAT_CHANNEL_STATUS.getName(),
                JSON_HELPER.serialize(chatChannelAggregate), buildHeaders(chatChannelAggregate));
        destination.send(document, documentDispatcher);
    }

    private Map<String, String> buildHeaders(final ChatChannelAggregate headerAggregate) {
        final Map<String, String> ret = new HashMap<String, String>();
        ret.put(ChatDocumentHeader.channelId.name(), headerAggregate.getId());
        if (headerAggregate.getLocationId() != null) {
            ret.put(ChatDocumentHeader.locationId.name(), headerAggregate.getLocationId());
        }
        return ret;
    }
}
