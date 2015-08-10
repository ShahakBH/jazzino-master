package com.yazino.platform.processor.chat;

import com.google.common.base.Function;
import com.yazino.platform.model.chat.ChatParticipant;

import java.math.BigDecimal;

public final class ChatParticipantWorker {
    private ChatParticipantWorker() {
        // utility class
    }

    public static final Function<ChatParticipant, BigDecimal> CONVERT_TO_PLAYER_ID
            = new Function<ChatParticipant, BigDecimal>() {
        @Override
        public BigDecimal apply(final ChatParticipant chatParticipant) {
            return chatParticipant.getPlayerId();
        }
    };
}
