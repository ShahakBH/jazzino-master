package com.yazino.platform.service.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.SessionKeyPersistenceRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.GlobalPlayerListRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
import com.yazino.platform.session.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingSessionServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123);
    private static final BigDecimal BALANCE = BigDecimal.valueOf(435);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(43534);
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String IP_ADDRESS = "10.9.8.7";
    private static final String EMAIL_ADDRESS = "aUser@somewhere";
    private static final String REFERRER = "aReferrer";
    private static final Platform PLATFORM = Platform.WEB;
    private static final String LOGIN_URL = "http://loginUrl";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final String SESSION_KEY = "aSessionKey";
    private static final HashMap<String, Object> CLIENT_CONTEXT = new HashMap<>();

    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private TransactionalSessionService transactionalSessionService;
    @Mock
    private InternalWalletService walletService;
    @Mock
    private GigaSpace sessionSpace;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private GlobalPlayerListRepository globalPlayerListRepository;
    @Mock
    private PlayerRepository playerRepository;

    private GigaspaceRemotingSessionService underTest;

    @Before
    public void setUp() {
        when(sequenceGenerator.next()).thenReturn(SESSION_ID);
        CLIENT_CONTEXT.put("unique_identifier", "unique identifier for device");
        when(playerRepository.findById(PLAYER_ID)).thenReturn(new Player(PLAYER_ID));

        underTest = new GigaspaceRemotingSessionService(sequenceGenerator, playerSessionRepository,
                transactionalSessionService, walletService, sessionSpace, globalPlayerListRepository, playerRepository);
    }

    @Test
    public void updatePlayerInformationShouldDelegateToTheTransactionPlayerSessionAuthService() {
        underTest.updatePlayerInformation(PLAYER_ID, "aNickName", "aPictureUrl");

        verify(transactionalSessionService).updatePlayerInformation(PLAYER_ID, "aNickName", "aPictureUrl");
    }

    @Test
    public void aPlayersSessionsCanBeRetrievedByPlayerId() {
        when(playerSessionRepository.findAllByPlayer(PLAYER_ID)).thenReturn(asList(aPlayerSession()));

        final Collection<Session> session = underTest.findAllByPlayer(PLAYER_ID);

        assertThat(session, is(equalTo((Collection<Session>) newHashSet(aSession()))));
    }

    @Test
    public void aSessionCanBeRetrievedByPlayerIdAndSessionKey() {
        when(playerSessionRepository.findByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY)).thenReturn(aPlayerSession());

        final Session session = underTest.findByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY);

        assertThat(session, is(equalTo(aSession())));
    }

    @Test
    public void aNonExistentPlayerSessionReturnsNull() {
        final Session session = underTest.findByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY);

        assertThat(session, is(nullValue()));
    }

    @Test
    public void aPlayerWithNoSessionsReturnsAnEmptyCollection() {
        final Collection<Session> session = underTest.findAllByPlayer(PLAYER_ID);

        assertThat(session, is(empty()));
    }

    @Test(expected = NullPointerException.class)
    public void findingAPlayerSessionRejectsANullPlayerId() {
        underTest.findAllByPlayer(null);
    }

    @Test
    public void countSessionsDelegatesToTheGlobalRepository() {
        when(playerSessionRepository.countPlayerSessions(false)).thenReturn(12345);

        assertThat(underTest.countSessions(false), is(equalTo(12345)));
    }

    @Test
    public void invalidatedSessionsAreRemovedFromTheRepository() {
        underTest.invalidateAllByPlayer(PLAYER_ID);

        verify(playerSessionRepository).removeAllByPlayer(PLAYER_ID);
    }

    @Test
    public void anInvalidatedSessionIsRemovedFromTheRepository() {
        underTest.invalidateByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY);

        verify(playerSessionRepository).removeByPlayerAndSessionKey(PLAYER_ID, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void invalidatedSessionsRejectsANullPlayerId() {
        underTest.invalidateAllByPlayer(null);
    }

    @Test
    public void creatingASessionShouldWriteItToTheRepository() throws WalletServiceException {
        when(walletService.getBalance(ACCOUNT_ID)).thenReturn(BALANCE);
        when(sequenceGenerator.next()).thenReturn(SESSION_ID);

        underTest.createSession(aPlayer(), PARTNER_ID, REFERRER, IP_ADDRESS, EMAIL_ADDRESS, PLATFORM, LOGIN_URL, CLIENT_CONTEXT);

        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository).save(sessionCaptor.capture());
        final PlayerSession session = sessionCaptor.getValue();

        assertThat(session, is(not(nullValue())));
        assertThat(session.getPlayerId(), is(equalTo(PLAYER_ID)));
        assertThat(session.getBalanceSnapshot(), is(equalTo(BALANCE)));
        assertThat(session.getEmail(), is(equalTo(EMAIL_ADDRESS)));
        assertThat(session.getPartnerId(), is(equalTo(PARTNER_ID)));
        assertThat(session.getNickname(), is(equalTo("aPlayer")));
        assertThat(session.getSessionId(), is(equalTo(SESSION_ID)));
    }

    @Test
    public void creatingASessionShouldIgnoreBalanceRetrievalFailures() throws WalletServiceException {
        when(walletService.getBalance(ACCOUNT_ID)).thenThrow(new WalletServiceException("anException"));

        underTest.createSession(aPlayer(), PARTNER_ID, REFERRER, IP_ADDRESS, EMAIL_ADDRESS, PLATFORM, LOGIN_URL, CLIENT_CONTEXT);

        final ArgumentCaptor<PlayerSession> sessionCaptor = ArgumentCaptor.forClass(PlayerSession.class);
        verify(playerSessionRepository).save(sessionCaptor.capture());
        final PlayerSession session = sessionCaptor.getValue();

        assertThat(session, is(not(nullValue())));
        assertThat(session.getBalanceSnapshot(), is(nullValue()));
    }

    @Test
    public void creatingASessionShouldWriteTheSessionKeyToTheSpace() throws WalletServiceException {
        underTest.createSession(aPlayer(), PARTNER_ID, REFERRER, IP_ADDRESS, EMAIL_ADDRESS, PLATFORM, LOGIN_URL, CLIENT_CONTEXT);

        final ArgumentCaptor<SessionKeyPersistenceRequest> sessionKeyCaptor
                = ArgumentCaptor.forClass(SessionKeyPersistenceRequest.class);
        verify(sessionSpace).write(sessionKeyCaptor.capture());
        final SessionKey sessionKey = sessionKeyCaptor.getValue().getSessionKey();

        assertThat(sessionKey, is(not(nullValue())));
        assertThat(sessionKey.getAccountId(), is(equalTo(ACCOUNT_ID)));
        assertThat(sessionKey.getIpAddress(), is(equalTo(IP_ADDRESS)));
        assertThat(sessionKey.getReferrer(), is(equalTo(REFERRER)));
        assertThat(sessionKey.getSessionKey(), is(not(nullValue())));
        assertThat(sessionKey.getSessionKey().length(), is(greaterThan(0)));
        assertThat(sessionKey.getPlatform(), is(PLATFORM.name()));
        assertThat(sessionKey.getLoginUrl(), is(LOGIN_URL));
        assertThat(sessionKey.getClientContext(), is(equalTo((Map<String, Object>) CLIENT_CONTEXT)));

    }

    @Test
    public void findSessionDelegatesToTheGlobalRepository() {
        final PagedData<PlayerSession> expectedPlayerSessions = new PagedData<>(
                34, 0, 120, asList(aPlayerSession()));
        when(playerSessionRepository.findAll(34)).thenReturn(expectedPlayerSessions);
        final PagedData<Session> expectedSessions = new PagedData<>(34, 0, 120, asList(aSession()));

        final PagedData<Session> sessions = underTest.findSessions(34);

        assertThat(sessions, is(equalTo(expectedSessions)));
    }

    private BasicProfileInformation aPlayer() {
        return new BasicProfileInformation(PLAYER_ID, "aPlayer", "aPicture", ACCOUNT_ID);
    }

    private PlayerSession aPlayerSession() {
        final PlayerSession playerSession = new PlayerSession(PLAYER_ID);
        playerSession.setPlatform(Platform.WEB);
        playerSession.setIpAddress("anIpAddress");
        playerSession.setSessionId(SESSION_ID);
        playerSession.setLocalSessionKey(SESSION_KEY);
        return playerSession;
    }

    private Session aSession() {
        return new Session(SESSION_ID, PLAYER_ID, null, Platform.WEB, "anIpAddress", SESSION_KEY, null, null, null,
                null, null, null, Collections.<String>emptySet());
    }

}
