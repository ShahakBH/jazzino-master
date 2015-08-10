package com.yazino.web.util;

import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CachedTest {
	private Cached<String> underTest;
	private Cached.Retriever<String> retriever;
	static final String DESCRIPTOR = "foo";
	static final int LEASE_TIME = 1000;
	private SettableTimeSource timeSource;

	@Before
	public void setUp() throws Exception {
		//noinspection unchecked
		retriever = mock(Cached.Retriever.class);
		timeSource = new SettableTimeSource(0);
		underTest = new Cached<String>(timeSource, LEASE_TIME, retriever);
	}

	@Test
	public void shouldRetrieveValueIfNull() throws Exception {
		when(retriever.retrieve(DESCRIPTOR)).thenReturn("bar");
		assertEquals("bar", underTest.getItem(DESCRIPTOR));
		verify(retriever).retrieve(DESCRIPTOR);
	}

	@Test
	public void shouldNotRetrieveValueTwiceWithinLeasePeriod() throws Exception {
		when(retriever.retrieve(DESCRIPTOR)).thenReturn("bar");
		String result = "bar";
		assertEquals(result, underTest.getItem(DESCRIPTOR));
		timeSource.addMillis(LEASE_TIME);
		assertEquals(result, underTest.getItem(DESCRIPTOR));
		verify(retriever, times(1)).retrieve(DESCRIPTOR);
	}
	
	@Test
	public void shouldNotRetrieveValueAfterLeaseTimeout() throws Exception {
		when(retriever.retrieve(DESCRIPTOR)).thenReturn("bar");
		String result = "bar";
		assertEquals(result, underTest.getItem(DESCRIPTOR));
		timeSource.addMillis(LEASE_TIME + 1);
		assertEquals(result, underTest.getItem(DESCRIPTOR));
		verify(retriever, times(2)).retrieve(DESCRIPTOR);
	}
}
