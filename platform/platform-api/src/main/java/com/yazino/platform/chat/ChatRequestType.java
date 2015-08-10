package com.yazino.platform.chat;

import com.yazino.platform.session.LocationChangeType;

import static org.apache.commons.lang3.Validate.notNull;

public enum ChatRequestType {
    ADD_PARTICIPANT("addParticipant"),
    LEAVE_CHANNEL("leaveChannel"),
    PUBLISH_CHANNEL("publishChannel"),
    SEND_MESSAGE("sendMessage"),
    LEAVE_ALL("leaveAll"),
    PUBLISH_CHANNELS("publishChannels");

    private final String id;

    private ChatRequestType(final String id) {
        notNull(id, "id may not be null");

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ChatRequestType findById(final String idToFind) {
        notNull(idToFind, "idToFind may not be null");

        for (ChatRequestType chatRequestType : values()) {
            if (chatRequestType.getId().equals(idToFind)) {
                return chatRequestType;
            }
        }

        return valueOf(idToFind);
    }

    public static ChatRequestType parse(final LocationChangeType notificationType) {
        switch (notificationType) {
            case ADD:
                return ChatRequestType.ADD_PARTICIPANT;
            case REMOVE:
                return ChatRequestType.LEAVE_CHANNEL;
            default:
                return null;
        }
    }
}
