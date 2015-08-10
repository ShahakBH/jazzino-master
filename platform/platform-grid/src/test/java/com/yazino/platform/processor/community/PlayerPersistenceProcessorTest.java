package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerPersistenceRequest;
import com.yazino.platform.persistence.community.PlayerDAO;
import com.yazino.platform.repository.community.PlayerRepository;
import org.junit.Before;
import org.junit.Test;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class PlayerPersistenceProcessorTest {
    private static final PlayerPersistenceRequest[] EMPTY_LIST = new PlayerPersistenceRequest[0];

    private PlayerRepository playerRepository;
    private GigaSpace communityGigaSpace;
    private PlayerPersistenceProcessor underTest;
    private BigDecimal playerId;
    private PlayerPersistenceRequest standardRequest;
    private Player player;
    private PlayerDAO playerDAO;

    @Before
    public void setUp() {
        underTest = new PlayerPersistenceProcessor();
        communityGigaSpace = mock(GigaSpace.class);
        underTest.setCommunityGigaSpace(communityGigaSpace);
        playerRepository = mock(PlayerRepository.class);
        underTest.setPlayerRepository(playerRepository);
        playerDAO = mock(PlayerDAO.class);
        underTest.setPlayerDAO(playerDAO);
        playerId = BigDecimal.ONE;
        standardRequest = new PlayerPersistenceRequest(playerId);
        player = mock(Player.class);
    }

    @Test
    public void testSuccessPathSavesPlayerRemovesDuplicatesAndReturnsNull() {
        when(player.getPlayerId()).thenReturn(playerId);
        when(playerRepository.findById(playerId)).thenReturn(player);

        expectRemovalOfMatchingRequests(standardRequest);

        PlayerPersistenceRequest response = underTest.processRequest(standardRequest);

        assertNull(response);
        verify(playerDAO).save(player);
    }

    @Test
    public void whenPlayerNotFoundMatchingRequestsAreRemovedAndNullIsReturned() {
        when(playerRepository.findById(playerId)).thenReturn(null);
        expectRemovalOfMatchingRequests(standardRequest);

        PlayerPersistenceRequest response = underTest.processRequest(standardRequest);

        assertNull(response);
    }

    @Test
    public void whenExceptionFindingPlayerThrownRequestIsReturnedInErrorStateAndNoSaveHappens() {
        when(playerRepository.findById(playerId)).thenThrow(new RuntimeException("foo"));

        PlayerPersistenceRequest response = underTest.processRequest(standardRequest);

        assertEquals(getCopyOfRequestInErrorState(standardRequest), response);
    }

    @Test
    public void whenExceptionOnSaveThrownRequestIsReturnedInErrorState() {
        when(playerRepository.findById(playerId)).thenReturn(player);
        doThrow(new RuntimeException("foo")).when(playerDAO).save(player);

        PlayerPersistenceRequest response = underTest.processRequest(standardRequest);

        assertEquals(getCopyOfRequestInErrorState(standardRequest), response);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionOnNullRequest() {
        underTest.processRequest(null);
    }

    private void expectRemovalOfMatchingRequests(PlayerPersistenceRequest request) {
        PlayerPersistenceRequest template = getCopyOfRequest(request);
        when(communityGigaSpace.takeMultiple(template, Integer.MAX_VALUE)).thenReturn(EMPTY_LIST);

        PlayerPersistenceRequest errorTemplate = getCopyOfRequestInErrorState(request);
        when(communityGigaSpace.takeMultiple(errorTemplate, Integer.MAX_VALUE)).thenReturn(EMPTY_LIST);
    }

    private PlayerPersistenceRequest getCopyOfRequestInErrorState(PlayerPersistenceRequest request) {
        final PlayerPersistenceRequest result = getCopyOfRequest(request);
        result.setStatus(PlayerPersistenceRequest.STATUS_ERROR);
        return result;
    }

    private PlayerPersistenceRequest getCopyOfRequest(PlayerPersistenceRequest request) {
        return new PlayerPersistenceRequest(request.getPlayerId());
    }
}
