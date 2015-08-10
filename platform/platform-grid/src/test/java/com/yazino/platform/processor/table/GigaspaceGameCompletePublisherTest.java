package com.yazino.platform.processor.table;

import com.yazino.platform.gamehost.GigaspaceGameCompletePublisher;
import com.yazino.platform.model.table.GameCompleted;
import com.yazino.platform.repository.table.GameRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceGameCompletePublisherTest {
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final BigDecimal TABLE_ID = BigDecimal.TEN;

    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameRules gameRules;
    @Mock
    private GameStatus gameStatus;

    private GigaspaceGameCompletePublisher underTest;

    @Before
    public void init() {
        when(gameRepository.getGameRules(GAME_TYPE)).thenReturn(gameRules);

        underTest = new GigaspaceGameCompletePublisher(gigaSpace, gameRepository);
    }

    @Test
    public void when_game_is_not_complete_nothing_published() {
        when(gameRules.isComplete(gameStatus)).thenReturn(false);
        underTest.publishCompletedGame(gameStatus, GAME_TYPE, TABLE_ID, CLIENT_ID);
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test(expected = NullPointerException.class)
    public void when_game_type_not_supplied_exception_thrown() {
        underTest.publishCompletedGame(gameStatus, null, TABLE_ID, CLIENT_ID);
    }

    @Test(expected = NullPointerException.class)
    public void when_tableid_not_supplied_exception_thrown() {
        underTest.publishCompletedGame(gameStatus, GAME_TYPE, null, CLIENT_ID);
    }

    @Test(expected = NullPointerException.class)
    public void when_status_not_supplied_exception_thrown() {
        underTest.publishCompletedGame(null, GAME_TYPE, TABLE_ID, CLIENT_ID);
    }

    @Test
    public void when_happy_game_status_is_published_to_the_space() {
        when(gameRules.isComplete(gameStatus)).thenReturn(true);
        underTest.publishCompletedGame(gameStatus, GAME_TYPE, TABLE_ID, CLIENT_ID);
        verify(gigaSpace).write(eq(new GameCompleted(gameStatus, GAME_TYPE, TABLE_ID, CLIENT_ID)));
    }
}
