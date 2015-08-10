package com.yazino.platform.service.tournament;

import com.yazino.platform.model.tournament.AwardMedalsRequest;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import com.yazino.platform.tournament.Summary;
import com.yazino.platform.tournament.TournamentTestService;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTournamentTestService implements TournamentTestService {

    private final TournamentSummaryRepository tournamentSummaryRepository;
    private final GigaSpace tournamentSpace;

    @Autowired
    public GigaspaceRemotingTournamentTestService(final TournamentSummaryRepository tournamentSummaryRepository,
                                                  @Qualifier("gigaSpace") final GigaSpace tournamentSpace) {
        notNull(tournamentSummaryRepository, "tournamentSummaryRepository may not be null");
        notNull(tournamentSpace, "tournamentSpace may not be null");

        this.tournamentSummaryRepository = tournamentSummaryRepository;
        this.tournamentSpace = tournamentSpace;
    }

    @Override
    public void fakeSummary(@Routing("getTournamentId") final Summary summary) {
        notNull(summary, "summary may not be null");

        final TournamentSummary tournamentSummary = new TournamentSummary();
        tournamentSummary.setTournamentId(summary.getTournamentId());
        tournamentSummary.setTournamentName(summary.getTournamentName());
        tournamentSummary.setFinishDateTime(summary.getFinishDateTime());
        tournamentSummary.setGameType(summary.getGameType());
        tournamentSummary.setPlayers(summary.getPlayers());

        tournamentSummaryRepository.delete(summary.getTournamentId());
        tournamentSummaryRepository.save(tournamentSummary);

        tournamentSpace.write(new AwardMedalsRequest(summary.getTournamentId(), summary.getGameType()));
    }

}
