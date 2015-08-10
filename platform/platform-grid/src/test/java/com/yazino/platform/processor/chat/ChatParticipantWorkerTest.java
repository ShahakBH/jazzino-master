package com.yazino.platform.processor.chat;

import com.yazino.platform.model.chat.ChatParticipant;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class ChatParticipantWorkerTest {
    @Test
    public void testInvokable() {
        BigDecimal playerId = BigDecimal.TEN;
        BigDecimal result = ChatParticipantWorker.CONVERT_TO_PLAYER_ID.apply(new ChatParticipant(playerId, "no important"));
        assertEquals(result, playerId);
    }
}
