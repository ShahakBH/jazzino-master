package com.yazino.platform.community;

import org.junit.Test;
import com.yazino.game.api.ParameterisedMessage;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the TrophyCabinet class.
 */
public class TrophyCabinetTest {

	private final TrophyCabinet trophyCabinet = new TrophyCabinet();

	@Test
	public void initiallyNoTrophiesExist() throws Exception {
		assertTrue(trophyCabinet.getTrophySummaries().isEmpty());
	}

	@Test(expected = NullPointerException.class)
	public void cannotAddSummaryWithNullValue() throws Exception {
		trophyCabinet.addTrophySummary(null);
	}

	@Test
	public void addTrophySummary() throws Exception {
		trophyCabinet.addTrophySummary(new TrophySummary("A", "FOO", new ParameterisedMessage("test"), 1));
		assertTrue(trophyCabinet.hasTrophySummary("A"));
		assertEquals(1, trophyCabinet.getTrophySummary("A").getCount());
	}

	@Test
	public void totalTrophiesForNames() {
		trophyCabinet.addTrophySummary(new TrophySummary("A", "image", new ParameterisedMessage("test"), 3));
		trophyCabinet.addTrophySummary(new TrophySummary("B", "image", new ParameterisedMessage("test"), 4));
		trophyCabinet.addTrophySummary(new TrophySummary("C", "image", new ParameterisedMessage("test"), 2));
		assertEquals(7, trophyCabinet.getTotalTrophies(Arrays.asList("A", "B")));
	}

}
