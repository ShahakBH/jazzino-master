package com.yazino.platform.processor.tournament;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link com.yazino.platform.processor.tournament.TournamentRegistrationInfoTransformer} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class TournamentRegistrationInfoTransformerTest {
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private TournamentVariationTemplate tournamentVariationTemplate;

    private final TournamentRegistrationInfoTransformer transformer = new TournamentRegistrationInfoTransformer();
    private final Tournament tournament = new Tournament();
    private static final BigDecimal ENTRY_FEE = BigDecimal.valueOf(999);
    private static final BigDecimal SERVICE_FEE = BigDecimal.valueOf(888);

    @Before
    public void setup() throws WalletServiceException {
        tournament.setTournamentStatus(TournamentStatus.REGISTERING);
        tournament.setTournamentId(BigDecimal.valueOf(123));
        tournament.setStartTimeStamp(new DateTime());
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        when(tournamentVariationTemplate.getEntryFee()).thenReturn(ENTRY_FEE);
        when(tournamentVariationTemplate.getServiceFee()).thenReturn(SERVICE_FEE);
    }

    @Test
    public void shouldHaveCorrectTournamentId() throws Exception {
        TournamentRegistrationInfo info = transformer.apply(tournament);
    }

    @Test
    public void shouldHaveCorrectStartTime() throws Exception {
        TournamentRegistrationInfo info = transformer.apply(tournament);
        assertEquals(tournament.getStartTimeStamp(), info.getStartTimeStamp());
    }

    @Test
    public void shouldHaveCorrectEntryFee() throws Exception {

        TournamentRegistrationInfo info = transformer.apply(tournament);
        assertEquals(ENTRY_FEE.add(SERVICE_FEE), info.getEntryFee());
    }

    @Test
    public void shouldHaveCorrectPlayerIds() throws Exception {

        BigDecimal playerId1 = BigDecimal.valueOf(11);
        BigDecimal playerId2 = BigDecimal.valueOf(22);
        setTournamentPlayers(playerId1, playerId2);
        TournamentRegistrationInfo info = transformer.apply(tournament);
        assertTrue(info.isRegistered(playerId1));
        assertTrue(info.isRegistered(playerId2));
    }

    private void setTournamentPlayers(BigDecimal... playersIds) throws Exception {
        TournamentHost tournamentHost = mock(TournamentHost.class);
        SettableTimeSource timeSource = new SettableTimeSource();
        when(tournamentHost.getTimeSource()).thenReturn(timeSource);
        when(tournamentHost.getInternalWalletService()).thenReturn(internalWalletService);
        when(internalWalletService.createAccount(isA(String.class))).thenReturn(BigDecimal.valueOf(33));
        timeSource.setMillis(tournament.getStartTimeStamp().minusMillis(2000).getMillis());
        tournament.setSignupStartTimeStamp(tournament.getStartTimeStamp().minusMillis(4000));
        when(tournamentVariationTemplate.getMaxPlayers()).thenReturn(playersIds.length + 1);
        for (BigDecimal playerId : playersIds) {
            tournament.addPlayer(new Player(playerId, "test" + playerId, playerId.multiply(BigDecimal.TEN), "aPictureUrl", null, null, null),
                    tournamentHost);
            tournament.getPlayers().getByPlayerId(playerId).setStatus(TournamentPlayerStatus.ACTIVE);
        }
    }
}
