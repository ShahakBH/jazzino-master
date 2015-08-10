package com.yazino.platform.processor.tournament;

import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PublishStatusRequest;
import com.yazino.platform.model.community.PublishStatusRequestType;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentOperationResult;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TournamentPlayerProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ACCOUNT_ID = BigDecimal.valueOf(30);
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(2000);
    private static final String TOURNAMENT_NAME = "aTournament";
    private static final String PARTNER_ID = "aPartner";
    private static final String PLAYER_NAME = "aPlayer";
    private static final long LEASE_TIME = 60000L;
    private static final String REQUEST_SPACE_ID = "aSpaceId";

    @Mock
    private WalletService walletService;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private Tournament registeringTournament;
    @Mock
    private TournamentHost tournamentHost;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;

    private TournamentPlayerProcessor underTest;
    private Player player;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new TournamentPlayerProcessor(tournamentHost, playerRepository,
                hostDocumentDispatcher, tournamentRepository, gigaSpace, destinationFactory);

        when(destinationFactory.player(PLAYER_ID)).thenReturn(destination);

        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(registeringTournament);
        when(tournamentRepository.lock(TOURNAMENT_ID)).thenReturn(registeringTournament);

        player = new Player(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, "aPictureUrl", null, null, null);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);

        when(registeringTournament.getTournamentVariationTemplate()).thenReturn(aTemplate());
        when(registeringTournament.getName()).thenReturn(TOURNAMENT_NAME);
        when(registeringTournament.getPartnerId()).thenReturn(PARTNER_ID);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void responsesHaveTheSpaceIdFromTheRequest() {
        final TournamentPlayerProcessingResponse response = register(TOURNAMENT_ID, PLAYER_ID);

        assertThat(response.getRequestSpaceId(), is(equalTo(REQUEST_SPACE_ID)));
    }

    @Test
    public void anErrorWhenRetrievingThePlayerReturnsUnknown() {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID)).thenThrow(new RuntimeException("aTestException"));

        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void anErrorWhenRetrievingTheTournamentReturnsUnknown() {
        reset(tournamentRepository);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenThrow(
                new RuntimeException("aTestException"));

        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void anAsynchronousRegistrationDoesNotSendAResponse() {
        final TournamentPlayerProcessingResponse response = process(
                TOURNAMENT_ID, PLAYER_ID, TournamentPlayerProcessingType.ADD, true);

        assertThat(response, is(nullValue()));
    }

    @Test
    public void anAsynchronousDeregistrationDoesNotSendAResponse() {
        final TournamentPlayerProcessingResponse response = process(
                TOURNAMENT_ID, PLAYER_ID, TournamentPlayerProcessingType.REMOVE, true);

        assertThat(response, is(nullValue()));
    }

    @Test
    public void registeringAPlayerAddsThePlayerToTheTournament() throws TournamentException {
        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.SUCCESS)));
        verify(registeringTournament).addPlayer(player, tournamentHost);
    }

    @Test
    public void anUnknownResponseIsReturnedWhenRegisteringForAnUnknownTournament() {
        final TournamentOperationResult result = resultOf(register(BigDecimal.valueOf(-1000), PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void anUnknownResponseIsReturnedWhenRegisteringAnUnknownPlayer() {
        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, BigDecimal.valueOf(-1000)));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void aTournamentExceptionDuringRegistrationPropagatesResult() throws TournamentException {
        doThrow(new TournamentException(TournamentOperationResult.AFTER_SIGNUP_TIME))
                .when(registeringTournament).addPlayer(player, tournamentHost);

        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.AFTER_SIGNUP_TIME)));
    }

    @Test
    public void aNonTournamentExceptionDuringRegistrationReturnsAnUnknownResult() throws TournamentException {
        doThrow(new RuntimeException("aTestException"))
                .when(registeringTournament).addPlayer(player, tournamentHost);

        final TournamentOperationResult result = resultOf(register(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void aSuccessfulRegistrationSendsABalancePublishRequest() {
        register(TOURNAMENT_ID, PLAYER_ID);

        verify(playerRepository).savePublishStatusRequest(new PublishStatusRequest(PLAYER_ID, PublishStatusRequestType.PLAYER_BALANCE));
    }

    @Test
    public void aSuccessfulRegistrationSendsANewsEvent() {
        when(registeringTournament.findPlayer(PLAYER_ID)).thenReturn(new TournamentPlayer(PLAYER_ID, PLAYER_NAME));

        register(TOURNAMENT_ID, PLAYER_ID);

        final ParameterisedMessage message = new ParameterisedMessage("%s has joined the \"%s\" tournament", player.getName(), TOURNAMENT_NAME);
        final NewsEvent event = new NewsEvent.Builder(player.getPlayerId(), message)
                .setType(NewsEventType.NEWS)
                .setImage("COMPETITION_JOIN_BLACKJACK")
                .build();
        verify(hostDocumentDispatcher).send(new NewsEventHostDocument(PARTNER_ID, event, destination));
    }

    @Test
    public void deregisteringAPlayerRemovesThePlayerFromTheTournament() throws TournamentException {
        registeringTournament.addPlayer(player, tournamentHost);

        final TournamentOperationResult result = resultOf(deregister(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.SUCCESS)));
        verify(registeringTournament).removePlayer(player, tournamentHost);
    }

    @Test
    public void anUnknownResponseIsReturnedWhenDeregisteringFromAnUnknownTournament() {
        final TournamentOperationResult result = resultOf(deregister(BigDecimal.valueOf(-1000), PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void anUnknownResponseIsReturnedWhenDeregisteringAnUnknownPlayer() {
        final TournamentOperationResult result = resultOf(deregister(TOURNAMENT_ID, BigDecimal.valueOf(-1000)));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void aTournamentExceptionDuringDeregistrationPropagatesResult() throws TournamentException {
        doThrow(new TournamentException(TournamentOperationResult.AFTER_SIGNUP_TIME))
                .when(registeringTournament).removePlayer(player, tournamentHost);

        final TournamentOperationResult result = resultOf(deregister(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.AFTER_SIGNUP_TIME)));
    }

    @Test
    public void aNonTournamentExceptionDuringDeregistrationReturnsAnUnknownResult() throws TournamentException {
        doThrow(new RuntimeException("aTestException"))
                .when(registeringTournament).removePlayer(player, tournamentHost);

        final TournamentOperationResult result = resultOf(deregister(TOURNAMENT_ID, PLAYER_ID));

        assertThat(result, is(equalTo(TournamentOperationResult.UNKNOWN)));
    }

    @Test
    public void aSuccessfulDeregistrationSendsABalancePublishRequest() {
        deregister(TOURNAMENT_ID, PLAYER_ID);

        verify(playerRepository).savePublishStatusRequest(new PublishStatusRequest(PLAYER_ID, PublishStatusRequestType.PLAYER_BALANCE));
    }

    @Test
    public void aSuccessfulDeregistrationDoesNotSendANewsEvent() {
        deregister(TOURNAMENT_ID, PLAYER_ID);

        verifyZeroInteractions(hostDocumentDispatcher);
    }

    private TournamentOperationResult resultOf(final TournamentPlayerProcessingResponse response) {
        if (response != null) {
            return response.getTournamentOperationResult();
        }
        return null;
    }

    private TournamentPlayerProcessingResponse register(final BigDecimal tournamentId,
                                                        final BigDecimal playerId) {
        return process(tournamentId, playerId, TournamentPlayerProcessingType.ADD);
    }

    private TournamentPlayerProcessingResponse deregister(final BigDecimal tournamentId,
                                                          final BigDecimal playerId) {
        return process(tournamentId, playerId, TournamentPlayerProcessingType.REMOVE);
    }

    private TournamentPlayerProcessingResponse process(final BigDecimal tournamentId,
                                                       final BigDecimal playerId,
                                                       final TournamentPlayerProcessingType processingType) {
        return process(tournamentId, playerId, processingType, false);
    }

    private TournamentPlayerProcessingResponse process(final BigDecimal tournamentId,
                                                       final BigDecimal playerId,
                                                       final TournamentPlayerProcessingType processingType,
                                                       final boolean async) {
        final TournamentPlayerProcessingRequest request = new TournamentPlayerProcessingRequest(
                playerId, tournamentId, processingType, async);
        request.setSpaceId(REQUEST_SPACE_ID);
        underTest.process(request);

        if (async) {
            verifyZeroInteractions(gigaSpace);
            return null;

        } else {
            final ArgumentCaptor<TournamentPlayerProcessingResponse> responseCaptor
                    = ArgumentCaptor.forClass(TournamentPlayerProcessingResponse.class);
            verify(gigaSpace).write(responseCaptor.capture(), eq(LEASE_TIME));
            return responseCaptor.getValue();
        }
    }

    private TournamentVariationTemplate aTemplate() {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(34345L))
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("templ1")
                .setEntryFee(BigDecimal.valueOf(1000))
                .setServiceFee(BigDecimal.ZERO)
                .setGameType("BLACKJACK")
                .toTemplate();
    }


}
