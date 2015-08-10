package com.yazino.bi.payment.worldpay;

import com.yazino.bi.payment.Chargeback;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.service.PlayerProfileService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;

import static com.yazino.platform.player.PlayerProfileStatus.ACTIVE;
import static com.yazino.platform.player.PlayerProfileStatus.BLOCKED;
import static com.yazino.platform.player.PlayerProfileStatus.CLOSED;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerChargebackHandlerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Mock
    private AsyncEmailService emailService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private PlayerChargebackHandler underTest;

    @Before
    public void setUp() throws Exception {
        when(yazinoConfiguration.getString("strata.email.from-address", "contact@yazino.com")).thenReturn("aFromAddress");
        when(yazinoConfiguration.getString("payment.worldpay.chargeback.email.subject", "{0}, important information about your Yazino account")).thenReturn("aSubject:{0}");
        when(yazinoConfiguration.getString("payment.worldpay.chargeback.email.template", "backoffice/chargeback.vm")).thenReturn("aTemplate");

        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile(ACTIVE));

        underTest = new PlayerChargebackHandler(emailService, playerProfileService, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void aNullChargebackThrowsANullPointerException() {
        underTest.handleChargeback(null);
    }

    @Test
    public void aChargebackBlocksThePlayer() {
        underTest.handleChargeback(aChargeback());

        verify(playerProfileService).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system", "Chargeback created: aReference");
    }

    @Test
    public void aChargebackDoesNotBlockAPlayerWhoIsAlreadyBlocked() {
        reset(playerProfileService);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile(BLOCKED));

        underTest.handleChargeback(aChargeback());

        verify(playerProfileService, times(0)).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system", "Chargeback created: aReference");
    }

    @Test
    public void aChargebackDoesNotBlockAPlayerWhoHasBeenClosed() {
        reset(playerProfileService);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile(CLOSED));

        underTest.handleChargeback(aChargeback());

        verify(playerProfileService, times(0)).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system", "Chargeback created: aReference");
    }

    @Test
    public void aChargebackNotifiesThePlayer() {
        underTest.handleChargeback(aChargeback());

        final HashMap<String, Object> properties = new HashMap<>();
        properties.put("chargebackReference", "aReference");
        properties.put("displayName", "aDisplayName");
        verify(emailService).send("anEmailAddress", "aFromAddress", "aSubject:aDisplayName", "aTemplate", properties);
    }

    @Test
    public void aChargebackDoesNotNotifyAnAlreadyBlockedPlayer() {
        reset(playerProfileService);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile(BLOCKED));

        underTest.handleChargeback(aChargeback());

        verifyZeroInteractions(emailService);
    }

    @Test
    public void aChargebackDoesNotNotifyAClosedPlayer() {
        reset(playerProfileService);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(aPlayerProfile(CLOSED));

        underTest.handleChargeback(aChargeback());

        verifyZeroInteractions(emailService);
    }

    @Test
    public void aChargebackWithAnUnknownPlayerIDCausesNoActions() {
        reset(playerProfileService);

        underTest.handleChargeback(aChargeback());

        verify(playerProfileService).findByPlayerId(PLAYER_ID);
        verifyNoMoreInteractions(playerProfileService);
        verifyZeroInteractions(emailService);
    }

    private PlayerProfile aPlayerProfile(final PlayerProfileStatus status) {
        final PlayerProfile playerProfile = new PlayerProfile(PLAYER_ID, "anEmailAddress", "aDisplayName", "aRealName", Gender.OTHER, "aCountry",
                "aFirstName", "aLastName", new DateTime(1980, 1, 1, 0, 0, 0), "aReferralId", "aProviderName", "aRpxProvider",
                "anExternalId", false);
        playerProfile.setStatus(status);
        return playerProfile;
    }

    private Chargeback aChargeback() {
        return new Chargeback("aReference", new DateTime(2013, 5, 3, 0, 0, 0), "anInternalTransactionId", new DateTime(2013, 4, 2, 0, 0, 0),
                PLAYER_ID, null, "arc", "aReason", "anAccountNumber", new BigDecimal("10.00"), Currency.getInstance("GBP"));
    }
}
