package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class GigaspaceChatRequestTest {
    @Test
    public void testPublishChannelIsValid() {
        GigaspaceChatRequest underTest = new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL, BigDecimal.ONE, "foo", null);
        Assert.assertTrue(underTest.isValid());
        underTest.setChannelId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setChannelId("foo");
        Assert.assertTrue(underTest.isValid());
        underTest.setPlayerId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setLocationId("foo");
        Assert.assertFalse(underTest.isValid());
        underTest.setLocationId(null);
        underTest.setPlayerId(BigDecimal.TEN);
        Assert.assertTrue(underTest.isValid());
        underTest.setRequestType(null);
        Assert.assertFalse(underTest.isValid());
    }

    @Test
    public void testLeaveChannelIsValid() {
        GigaspaceChatRequest underTest = new GigaspaceChatRequest(ChatRequestType.LEAVE_CHANNEL, BigDecimal.ONE, "foo", null);
        Assert.assertTrue(underTest.isValid());
        underTest.setChannelId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setChannelId("foo");
        Assert.assertTrue(underTest.isValid());
        underTest.setPlayerId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setLocationId("foo");
        Assert.assertFalse(underTest.isValid());
        underTest.setLocationId(null);
        underTest.setPlayerId(BigDecimal.TEN);
        Assert.assertTrue(underTest.isValid());
        underTest.setRequestType(null);
        Assert.assertFalse(underTest.isValid());
    }

    @Test
    public void testAddParticipantIsValid() {
        GigaspaceChatRequest underTest = new GigaspaceChatRequest(ChatRequestType.ADD_PARTICIPANT, BigDecimal.ONE, "foo", null);
        Assert.assertFalse(underTest.isValid());
        underTest.getArgs().put(ChatRequestArgument.PLAYER_ID, "ss");
        Assert.assertFalse(underTest.isValid());
        underTest.getArgs().put(ChatRequestArgument.NICKNAME, "ss");
        Assert.assertTrue(underTest.isValid());
        underTest.setChannelId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setChannelId("foo");
        Assert.assertTrue(underTest.isValid());
        underTest.setPlayerId(null);
        Assert.assertFalse(underTest.isValid());
        underTest.setLocationId("foo");
        Assert.assertTrue(underTest.isValid());
        underTest.setLocationId(null);
        underTest.setPlayerId(BigDecimal.TEN);
        Assert.assertTrue(underTest.isValid());
        underTest.setRequestType(null);
        Assert.assertFalse(underTest.isValid());
    }
}
