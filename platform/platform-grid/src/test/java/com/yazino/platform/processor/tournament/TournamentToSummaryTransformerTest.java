package com.yazino.platform.processor.tournament;

import com.google.common.collect.Sets;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link com.yazino.platform.processor.tournament.TournamentToSummaryTransformer} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class TournamentToSummaryTransformerTest {

    @Mock
    private PlayerRepository playerRepository;

    private TournamentToSummaryTransformer underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        underTest = new TournamentToSummaryTransformer(playerRepository);

        for (int i = 1; i < 6; ++i) {
            when(playerRepository.findById(BigDecimal.valueOf(i))).thenReturn(
                    new Player(BigDecimal.valueOf(i), "player" + i, BigDecimal.valueOf(i), "picture" + i, null, null, null));
        }
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aNullTournamentReturnsANullSummary() {
        assertThat(underTest.apply(null), is(nullValue()));
    }

    @Test
    public void tournamentSummaryShouldIgnorePlayersWithANullPosition() {
        final Tournament tournament = aTournamentWithPlayers(
                aPlayer(1, 1, 10),
                aPlayer(2, 3, 2),
                aPlayer(3, 2, 5),
                aPlayer(4, 3, 2),
                aPlayer(5, null, 0));

        final TournamentSummary summary = underTest.apply(tournament);

        assertNotNull(summary);
        final List<TournamentPlayerSummary> expected = asList(
                aPlayerSummaryOf(1, 1, 10),
                aPlayerSummaryOf(3, 2, 5),
                aPlayerSummaryOf(2, 3, 2),
                aPlayerSummaryOf(4, 3, 2));
        assertEquals(expected, summary.playerSummaries());
    }

    @Test
    public void tournamentSummaryShouldBeGeneratedWithOnlyFirstToThirdPlacedWinners() {
        final Tournament tournament = aTournamentWithPlayers(
                aPlayer(1, 1, 10),
                aPlayer(2, 3, 2),
                aPlayer(3, 2, 5),
                aPlayer(4, 3, 2),
                aPlayer(5, 4, 0));

        final Date preDate = new Date();

        final TournamentSummary summary = underTest.apply(tournament);

        final Date postDate = new Date();

        assertNotNull(summary);
        assertEquals(tournament.getTournamentId(), summary.getTournamentId());
        assertEquals(tournament.getName(), summary.getTournamentName());
        assertTrue(summary.getFinishDateTime().equals(preDate) || summary.getFinishDateTime().after(preDate));
        assertTrue(summary.getFinishDateTime().equals(postDate) || summary.getFinishDateTime().before(postDate));

        final List<TournamentPlayerSummary> expected = asList(
                aPlayerSummaryOf(1, 1, 10),
                aPlayerSummaryOf(3, 2, 5),
                aPlayerSummaryOf(2, 3, 2),
                aPlayerSummaryOf(4, 3, 2),
                aPlayerSummaryOf(5, 4, 0));
        assertEquals(expected, summary.playerSummaries());
    }

    private TournamentPlayerSummary aPlayerSummaryOf(final int id,
                                                     final int position,
                                                     final int prize) {
        return new TournamentPlayerSummary(BigDecimal.valueOf(id), position,
                "Player " + id, BigDecimal.valueOf(prize), "picture" + id);
    }

    private TournamentVariationTemplate aTournamentTemplate() {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(new BigDecimal(34345L))
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("TestTemplate")
                .setGameType("BLACKJACK")
                .setEntryFee(BigDecimal.valueOf(10))
                .setServiceFee(BigDecimal.valueOf(5))
                .toTemplate();
    }

    private TournamentPlayer aPlayer(final int id,
                                     final Integer position,
                                     final int settledPrize) {
        final TournamentPlayer player = new TournamentPlayer(BigDecimal.valueOf(id), "Player " + id);
        player.setLeaderboardPosition(position);
        player.setSettledPrize(BigDecimal.valueOf(settledPrize));
        return player;
    }

    private Tournament aTournamentWithPlayers(final TournamentPlayer... players) {
        Tournament tournament = new Tournament(BigDecimal.valueOf(18L), Sets.newHashSet(players));
        tournament.setTournamentVariationTemplate(aTournamentTemplate());
        tournament.setTournamentStatus(TournamentStatus.SETTLED);
        tournament.setStartTimeStamp(new DateTime());
        tournament.setName("TestTournament");
        tournament.setPartnerId("YAZINO");
        tournament.setPot(BigDecimal.ZERO);
        tournament.setTournamentLeaderboard(mock(TournamentLeaderboard.class));
        return tournament;
    }

}
