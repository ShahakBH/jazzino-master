package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerLastPlayedUpdateProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(234);
    private static final DateTime LAST_PLAYED = new DateTime(3242454235453L);
    private static final BigDecimal NON_EXISTENT_PLAYER_ID = BigDecimal.valueOf(-1);

    @Mock
    private PlayerRepository playerRepository;

    private PlayerLastPlayedUpdateProcessor underTest;

    @Before
    public void setUp() {
        underTest = new PlayerLastPlayedUpdateProcessor(playerRepository);

        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer());
        when(playerRepository.lock(PLAYER_ID)).thenReturn(aPlayer());
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullRepoistory() {
        new PlayerLastPlayedUpdateProcessor(null);
    }

    @Test(expected = IllegalStateException.class)
    public void logicCannotBeCalledOnACGLibInstance() {
        new PlayerLastPlayedUpdateProcessor().process(aRequest());
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(playerRepository);
    }

    @Test
    public void aRequestForANonExistentPlayerIsIgnored() {
        underTest.process(aRequestForANonExistentPlayer());

        verify(playerRepository).findById(NON_EXISTENT_PLAYER_ID);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    public void aRequestForAPlayerThatCannotBeLockedIsIgnored() {
        when(playerRepository.lock(PLAYER_ID)).thenThrow(new ConcurrentModificationException());

        try {
            underTest.process(aRequest());
            fail("Expected exception not thrown");
        } catch (ConcurrentModificationException e) {
            // expected
        }

        verify(playerRepository).findById(PLAYER_ID);
        verify(playerRepository).lock(PLAYER_ID);
        verifyNoMoreInteractions(playerRepository);
    }

    @Test
    public void aRequestSavesTheUpdatedPlayer() {
        underTest.process(aRequest());

        verify(playerRepository).saveLastPlayed(anUpdatedPlayer());
    }

    @Test
    public void anExceptionDuringSavingIsNotPropagated() {
        doThrow(new NullPointerException("aTestException")).when(playerRepository).save(anUpdatedPlayer());

        underTest.process(aRequest());
    }

    private PlayerLastPlayedUpdateRequest aRequest() {
        return new PlayerLastPlayedUpdateRequest(PLAYER_ID, LAST_PLAYED);
    }

    private PlayerLastPlayedUpdateRequest aRequestForANonExistentPlayer() {
        return new PlayerLastPlayedUpdateRequest(NON_EXISTENT_PLAYER_ID, LAST_PLAYED);
    }

    private Player aPlayer() {
        return new Player(PLAYER_ID);
    }

    private Player anUpdatedPlayer() {
        final Player player = new Player(PLAYER_ID);
        player.setLastPlayed(LAST_PLAYED);
        return player;
    }
}
