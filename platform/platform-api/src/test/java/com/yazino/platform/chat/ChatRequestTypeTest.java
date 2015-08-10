package com.yazino.platform.chat;

import com.yazino.platform.session.LocationChangeType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChatRequestTypeTest {
	@Test
	public void testParse() {
		assertEquals(ChatRequestType.ADD_PARTICIPANT, ChatRequestType.parse(LocationChangeType.ADD));
		assertEquals(ChatRequestType.LEAVE_CHANNEL, ChatRequestType.parse(LocationChangeType.REMOVE));
		assertNull(ChatRequestType.parse(LocationChangeType.LOG_ON));
	}
}
