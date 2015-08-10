package com.yazino.platform.model.chat;

import java.io.Serializable;

public enum ChatChannelType implements Serializable {
    table(false, false), personal(true, true);
    private final boolean canAddParticipants;
    private final boolean canParticipantsLeave;

    ChatChannelType(final boolean canAddParticipants,
                    final boolean canParticipantsLeave) {
        this.canAddParticipants = canAddParticipants;
        this.canParticipantsLeave = canParticipantsLeave;
    }

    public boolean canAddParticipants() {
        return canAddParticipants;
    }

    public boolean canParticipantsLeave() {
        return canParticipantsLeave;
    }
}
