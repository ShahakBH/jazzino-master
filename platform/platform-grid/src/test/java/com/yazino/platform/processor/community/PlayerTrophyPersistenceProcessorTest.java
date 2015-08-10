package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.model.community.PlayerTrophyPersistenceRequest;
import com.yazino.platform.persistence.community.PlayerTrophyDAO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

/**
 * Tests the {@link com.yazino.platform.processor.community.PlayerTrophyPersistenceProcessor} class.
 */
public class PlayerTrophyPersistenceProcessorTest {

    private final PlayerTrophyPersistenceProcessor processor = new PlayerTrophyPersistenceProcessor();
    private final PlayerTrophyDAO dao = mock(PlayerTrophyDAO.class);

    @Before
    public void setup() {
        processor.setPlayerTrophyDAO(dao);
    }

    @Test(expected = NullPointerException.class)
    public void ensureCannotProcessNullRequest() throws Exception {
        processor.processPlayerTrophyPersistenceRequest(null);
    }

    @Test
    public void ensurePlayerTrophyWrittenToDAO() throws Exception {
        BigDecimal playerId = BigDecimal.valueOf(40);
        BigDecimal trophyId = BigDecimal.valueOf(24);
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy = new PlayerTrophy(playerId, trophyId, dateTime);
        PlayerTrophyPersistenceRequest request = new PlayerTrophyPersistenceRequest();
        request.setPlayerTrophy(playerTrophy);
        processor.processPlayerTrophyPersistenceRequest(request);
        verify(dao).insert(playerTrophy);
    }

    @Test
    public void ensureRequestStatusIsUpdatedWhenSuccessful() throws Exception {
        BigDecimal playerId = BigDecimal.valueOf(40);
        BigDecimal trophyId = BigDecimal.valueOf(24);
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy = new PlayerTrophy(playerId, trophyId, dateTime);
        PlayerTrophyPersistenceRequest request = new PlayerTrophyPersistenceRequest();
        request.setPlayerTrophy(playerTrophy);
        processor.processPlayerTrophyPersistenceRequest(request);
    }

    @Test
    public void ensureRequestStatusIsUpdatedWhenFailed() throws Exception {
        BigDecimal playerId = BigDecimal.valueOf(40);
        BigDecimal trophyId = BigDecimal.valueOf(24);
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy = new PlayerTrophy(playerId, trophyId, dateTime);
        PlayerTrophyPersistenceRequest request = new PlayerTrophyPersistenceRequest();
        request.setPlayerTrophy(playerTrophy);
        doThrow(new DataIntegrityViolationException("Test")).when(dao).insert(any(PlayerTrophy.class));
        processor.processPlayerTrophyPersistenceRequest(request);
    }


}