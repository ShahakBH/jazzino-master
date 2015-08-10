package com.yazino.platform.model.tournament;

import com.yazino.platform.processor.tournament.TournamentPayoutCalculatorTest;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationPayout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class TournamentFactory {

    public static Tournament createSimpleTournament() {
        Set<TournamentPlayer> players = TournamentPayoutCalculatorTest.createPlayers(10);
        Tournament tournament = new Tournament();
        TournamentPlayers tournamentPlayers = new TournamentPlayers();
        tournamentPlayers.addAll(players);
        tournament.setPlayers(tournamentPlayers);
        return tournament;
    }

    public static Tournament createTournament(BigDecimal prizePool, List<TournamentVariationPayout> payouts, List<TournamentPlayer> players) {
        Tournament tournament = createSimpleTournament();

        TournamentVariationTemplateBuilder templateBuilder = new TournamentVariationTemplateBuilder();
        templateBuilder.setTournamentVariationTemplateId(new BigDecimal("1"));
        templateBuilder.setTournamentPayouts(payouts);
        templateBuilder.setPrizePool(prizePool);
        templateBuilder.setTournamentType(TournamentType.PRESET);
        templateBuilder.setTemplateName("Test Template");
        tournament.setTournamentVariationTemplate(templateBuilder.toTemplate());

        TournamentPlayers tournamentPlayers = new TournamentPlayers();
        tournamentPlayers.addAll(players);
        tournament.setPlayers(tournamentPlayers);

        return tournament;
    }
}
