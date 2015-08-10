package com.yazino.platform.model.conversion;

import com.google.common.base.Function;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.tournament.TournamentMonitorView;

public class TournamentMonitorViewTransformer implements Function<Tournament, TournamentMonitorView> {
    @Override
    public TournamentMonitorView apply(final Tournament tournament) {
        if (tournament == null) {
            return null;
        }

        return new TournamentMonitorView(
                tournament.getTournamentId(),
                tournament.getName(),
                tournament.getTournamentVariationTemplate().getGameType(),
                tournament.getTournamentVariationTemplate().getTemplateName(),
                tournament.getTournamentStatus(),
                tournament.getMonitoringMessage());
    }
}
