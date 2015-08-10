package com.yazino.platform.processor.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.model.session.GlobalPlayer;
import com.yazino.platform.model.session.GlobalPlayerList;
import com.yazino.platform.model.session.GlobalPlayerListUpdateRequest;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.repository.session.GlobalPlayerListRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.InvalidPlayerSessionException;
import com.yazino.platform.session.Location;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.table.TableType.PUBLIC;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalPlayerListUpdateProcessorTest {
    private GlobalPlayerListUpdateProcessor underTest;
    private PlayerSessionRepository playerSessionRepository;
    private GlobalPlayerListUpdateRequest request;
    private PlayerSession playerSession;
    private GlobalPlayerList globalPlayerList;

    @Before
    public void init() {
        request = new GlobalPlayerListUpdateRequest(new BigDecimal("1"));
        PlayerSession playerSession1 = new PlayerSession(BigDecimal.valueOf(3141592), BigDecimal.valueOf(1),
                "sessionKey", "pictureUrl", "nickname", Partner.YAZINO, Platform.WEB, "127.0.0.1", null, "playerEmail");
        for (int i = 1; i <= 1; i++) {
            playerSession1.addLocation(new Location(String.valueOf(i), "locationName", "gameType", null, PUBLIC));
        }
        playerSession = playerSession1;
        globalPlayerList = new GlobalPlayerList();

        GlobalPlayerListRepository globalPlayerListRepository = mock(GlobalPlayerListRepository.class);
        playerSessionRepository = mock(PlayerSessionRepository.class);
        underTest = new GlobalPlayerListUpdateProcessor(globalPlayerListRepository, playerSessionRepository);
        when(globalPlayerListRepository.read()).thenReturn(globalPlayerList);
        when(playerSessionRepository.findAllByPlayer(playerSession.getPlayerId())).thenReturn(asList(playerSession));
    }

    @Test
    public void onLocationChangeForPlayerInNewGameTypeTheListIsModified() throws InvalidPlayerSessionException {
        globalPlayerList.playerLocationChanged(request.getPlayerId(), asList(playerSession), playerSessionRepository);
        playerSession.addLocation(new Location(String.valueOf(2), "locationName", "gameType2", null, PUBLIC));
        underTest.process(request);
        assertThat(globalPlayerList.retrievePlayerList("gameType2").getCurrentList(), hasItem(new GlobalPlayer("gameType2", playerSession.getPlayerId(), playerSession.getNickname(), playerSession.getPictureUrl(), playerSession.getBalanceSnapshot(), playerSession.getLocations())));
    }

    @Test
    public void onLocationChangeForPlayerInSameGameTypeInTheListIsModified() throws InvalidPlayerSessionException {
        playerSession.addLocation(new Location(String.valueOf(2), "locationName", "gameType2", null, PUBLIC));
        globalPlayerList.playerLocationChanged(request.getPlayerId(), asList(playerSession), playerSessionRepository);
        underTest.process(request);
        assertThat(globalPlayerList.retrievePlayerList("gameType").getCurrentList(), hasItem(new GlobalPlayer("gameType", playerSession.getPlayerId(), playerSession.getNickname(), playerSession.getPictureUrl(), playerSession.getBalanceSnapshot(), playerSession.getLocations())));
        assertThat(globalPlayerList.retrievePlayerList("gameType2").getCurrentList(), hasItem(new GlobalPlayer("gameType2", playerSession.getPlayerId(), playerSession.getNickname(), playerSession.getPictureUrl(), playerSession.getBalanceSnapshot(), playerSession.getLocations())));
    }

    @Test
    public void onPlayerInListGoingOfflineTheListIsUpdated() throws InvalidPlayerSessionException {
        when(playerSessionRepository.findAllByPlayer(playerSession.getPlayerId())).thenReturn(Collections.<PlayerSession>emptySet());
        globalPlayerList.playerLocationChanged(request.getPlayerId(), asList(playerSession), playerSessionRepository);
        underTest.process(request);
        assertThat(globalPlayerList.retrievePlayerList("gameType").getCurrentList(), not(hasItem(new GlobalPlayer("gameType", playerSession.getPlayerId(), playerSession.getNickname(), playerSession.getPictureUrl(), playerSession.getBalanceSnapshot(), playerSession.getLocations()))));
    }


}
