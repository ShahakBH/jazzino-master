package com.yazino.platform.processor.community;

import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.UpdatePlayerRequest;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"UnusedDeclaration"})
@RunWith(MockitoJUnitRunner.class)
public class UpdatePlayerProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TransactionalSessionService transactionalSessionService;
    private Player player = new Player();
    @Mock
    private QueuePublishingService<PlayerEvent> playerEventService;

    private UpdatePlayerRequest updateRequest;
    private UpdatePlayerProcessor underTest;

    @Before
    public void setUp() {
        final PaymentPreferences paymentPreferences = new PaymentPreferences(Currency.EUR);
        updateRequest = new UpdatePlayerRequest(PLAYER_ID, "Jack", "picture location", paymentPreferences);
        underTest = new UpdatePlayerProcessor(playerRepository, transactionalSessionService);
    }

    @Test
    public void successfulyUpdatesPlayerPropagatingDisplayNameChangeAndPictureLocationChange() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);
        player.setPlayerId(BigDecimal.ONE);
        player.setName(updateRequest.getDisplayName());
        player.setPictureUrl(updateRequest.getPictureLocation());
        final PaymentPreferences paymentPreferences = new PaymentPreferences(updateRequest.getPaymentPreferences().getCurrency());
        player.setPaymentPreferences(paymentPreferences);

        underTest.processRequest(updateRequest);
        verify(playerRepository).save(player);
        verify(transactionalSessionService).updatePlayerInformation(
                PLAYER_ID, updateRequest.getDisplayName(), updateRequest.getPictureLocation());
    }

    @Test
    public void failsIfPlayerCouldNotBeFound() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(null);
        UpdatePlayerRequest templateRequest = new UpdatePlayerRequest();
        templateRequest.setPlayerId(updateRequest.getPlayerId());

        underTest.processRequest(updateRequest);
    }
}
