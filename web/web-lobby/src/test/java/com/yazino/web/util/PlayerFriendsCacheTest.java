package com.yazino.web.util;

import com.yazino.web.service.PlayerDetailsLobbyService;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PlayerFriendsCacheTest {
	private PlayerFriendsCache underTest;
	private PlayerDetailsLobbyService playerDetailsLobbyService;
	static final BigDecimal PLAYER_ID = BigDecimal.ONE;
	static final int LEASE_TIME = 100;
	private SettableTimeSource timeSource;

	@Before
	public void setUp() throws Exception {
		playerDetailsLobbyService = mock(PlayerDetailsLobbyService.class);
		underTest = new PlayerFriendsCache(playerDetailsLobbyService);
		underTest.setLeaseTime(LEASE_TIME);
		timeSource = new SettableTimeSource();
		underTest.setTimeSource(timeSource);
	}

	@Test
	public void shouldUsePlayerDetailsLobbyServiceToGetFriendIds() throws Exception {
		underTest.getFriendIds(PLAYER_ID);
		verify(playerDetailsLobbyService).getFriends(PLAYER_ID);
	}

	@Test
	public void shouldUseCachedValueAfterFirstCall() throws Exception {
		underTest.getFriendIds(PLAYER_ID);
		timeSource.addMillis(LEASE_TIME);
		underTest.getFriendIds(PLAYER_ID);
		verify(playerDetailsLobbyService, times(1)).getFriends(PLAYER_ID);
	}

	@Test
	public void shouldGetFriendsAgainOnceLeaseTimeOut() throws Exception {
		underTest.getFriendIds(PLAYER_ID);
		timeSource.addMillis(LEASE_TIME + 1);
		underTest.getFriendIds(PLAYER_ID);
		verify(playerDetailsLobbyService, times(2)).getFriends(PLAYER_ID);
	}

	@Test
	public void shouldGetFriendsAgainIfPlayerIdChanges() throws Exception {
		underTest.getFriendIds(PLAYER_ID);
		BigDecimal newId = PLAYER_ID.add(BigDecimal.ONE);
		underTest.getFriendIds(newId);
		verify(playerDetailsLobbyService, times(1)).getFriends(PLAYER_ID);
		verify(playerDetailsLobbyService, times(1)).getFriends(newId);
	}
	
	@Test
	public void shouldReturnFriendIds() throws Exception {
		HashSet<BigDecimal> friendIds = buildFriendIds(2);
		when(playerDetailsLobbyService.getFriends(PLAYER_ID)).thenReturn(friendIds);
		assertEquals(friendIds, underTest.getFriendIds(PLAYER_ID));
	}


	@Test
	public void shouldReturnFriendIdsFromCache() throws Exception {
		HashSet<BigDecimal> friendIds = buildFriendIds(2);
		when(playerDetailsLobbyService.getFriends(PLAYER_ID)).thenReturn(friendIds);
		underTest.getFriendIds(PLAYER_ID);
		assertEquals(friendIds, underTest.getFriendIds(PLAYER_ID));
	}
	
	private HashSet<BigDecimal> buildFriendIds(int numberOfFriends) {
		HashSet<BigDecimal> friendIds = new HashSet<BigDecimal>();
		for (int i = 0; i < numberOfFriends; i++) {
			friendIds.add(BigDecimal.valueOf(i));
		}
		return friendIds;
	}
}


