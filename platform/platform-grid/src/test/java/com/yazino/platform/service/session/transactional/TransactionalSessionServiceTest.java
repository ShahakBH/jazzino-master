package com.yazino.platform.service.session.transactional;

import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChange;
import com.yazino.platform.session.LocationChangeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.table.TableType.PRIVATE;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionalSessionServiceTest {
    private static final Location LOCATION = new Location("aLocation", "aLocationName", "aGameType", BigDecimal.valueOf(324), PRIVATE);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(423);
    private static final BigDecimal BALANCE = BigDecimal.valueOf(435);
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(200);
    private static final String SESSION_KEY = "firstSession";
    private static final String PICTURE_URL = "aPictureUrl";
    private static final String NICKNAME = "aNickname";

    @Mock
    private PlayerSessionRepository playerSessionRepository;

    private TransactionalSessionService underTest;

    @Before
    public void setUp() {
        underTest = new TransactionalSessionService(playerSessionRepository);

        when(playerSessionRepository.findAllByPlayer(PLAYER_ID)).thenReturn(asList(firstSession(), secondSession()));
        when(playerSessionRepository.findByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY)).thenReturn(firstSession());
        when(playerSessionRepository.lock(PLAYER_ID, SESSION_KEY)).thenReturn(firstSession());
        when(playerSessionRepository.lock(PLAYER_ID, "secondSession")).thenReturn(secondSession());
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void updateShouldRejectANullNotification() {
        underTest.updateSession(null, BALANCE);
    }

    @Test
    public void nonExistentPlayersAreIgnored() {
        final LocationChange notification = new LocationChange(
                BigDecimal.ZERO, SESSION_ID, locationChange().getType(), locationChange().getLocation());

        underTest.updateSession(notification, BALANCE);

        verify(playerSessionRepository).findAllByPlayer(BigDecimal.ZERO);
        verifyNoMoreInteractions(playerSessionRepository);
    }

    @Test
    public void nonExistentPlayersReturnANullSession() {
        final LocationChange notification = new LocationChange(
                BigDecimal.ZERO, SESSION_ID, locationChange().getType(), locationChange().getLocation());

        final PlayerSessionsSummary playerSession = underTest.updateSession(notification, BALANCE);

        assertThat(playerSession, is(nullValue()));
    }

    @Test
    public void theSessionTimestampShouldBeUpdated() {
        final Date currentDate = new Date();

        underTest.updateSession(locationChange(), BALANCE);

        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository, times(2)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues().get(0).getTimestamp(), is(greaterThanOrEqualTo(currentDate)));
        assertThat(sessionCaptor.getAllValues().get(1).getTimestamp(), is(greaterThanOrEqualTo(currentDate)));
    }

    @Test
    public void theBalanceShouldBeUpdatedIfNotNull() {
        final PlayerSessionsSummary playerSession = underTest.updateSession(locationChange(), BALANCE);

        assertThat(playerSession.getBalanceSnapshot(), is(equalTo(BALANCE)));
        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository, times(2)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues().get(0).getBalanceSnapshot(), is(equalTo(BALANCE)));
        assertThat(sessionCaptor.getAllValues().get(1).getBalanceSnapshot(), is(equalTo(BALANCE)));
    }

    @Test
    public void theBalanceShouldNotBeUpdatedIfNull() {
        final PlayerSessionsSummary playerSession = underTest.updateSession(locationChange(), null);

        assertThat(playerSession.getBalanceSnapshot(), is(equalTo(INITIAL_BALANCE)));
        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository, times(2)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues().get(0).getBalanceSnapshot(), is(equalTo(INITIAL_BALANCE)));
        assertThat(sessionCaptor.getAllValues().get(1).getBalanceSnapshot(), is(equalTo(INITIAL_BALANCE)));
    }

    @Test
    public void theSessionShouldBeSavedAfterUpdate() {
        underTest.updateSession(locationChange(), BALANCE);

        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository, times(2)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues().get(0).getLocations(), is(equalTo((Set<Location>) newHashSet(locationChange().getLocation()))));
        assertThat(sessionCaptor.getAllValues().get(1).getLocations(), is(nullValue()));
    }


    @Test
    public void playerInformationShouldBeUpdatedForOnlinePlayers() {
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);
        when(playerSessionRepository.lock(PLAYER_ID, SESSION_KEY)).thenReturn(firstSession());

        underTest.updatePlayerInformation(PLAYER_ID, NICKNAME, PICTURE_URL);

        final PlayerSession expectedSession = firstSession();
        expectedSession.setNickname(NICKNAME);
        expectedSession.setPictureUrl(PICTURE_URL);
        verify(playerSessionRepository).save(expectedSession);
    }

    @Test
    public void playerInformationShouldNotBeUpdatedForOfflinePlayers() {
        when(playerSessionRepository.findAllByPlayer(PLAYER_ID)).thenReturn(Collections.<PlayerSession>emptySet());

        underTest.updatePlayerInformation(PLAYER_ID, NICKNAME, PICTURE_URL);

        verify(playerSessionRepository).findAllByPlayer(PLAYER_ID);
        verifyNoMoreInteractions(playerSessionRepository);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void playerInformationUpdatesShouldRejectANullPlayerId() {
        underTest.updatePlayerInformation(null, NICKNAME, PICTURE_URL);
    }

    private LocationChange locationChange() {
        return new LocationChange(PLAYER_ID, SESSION_ID, LocationChangeType.ADD, LOCATION);
    }

    private PlayerSession firstSession() {
        final PlayerSession session = new PlayerSession(PLAYER_ID);
        session.setTimestamp(new Date(1000));
        session.setBalanceSnapshot(INITIAL_BALANCE);
        session.setSessionId(SESSION_ID);
        session.setLocalSessionKey(SESSION_KEY);
        return session;
    }

    private PlayerSession secondSession() {
        final PlayerSession session = new PlayerSession(PLAYER_ID);
        session.setTimestamp(new Date(1100));
        session.setBalanceSnapshot(INITIAL_BALANCE);
        session.setSessionId(BigDecimal.valueOf(2));
        session.setLocalSessionKey("secondSession");
        return session;
    }


}
