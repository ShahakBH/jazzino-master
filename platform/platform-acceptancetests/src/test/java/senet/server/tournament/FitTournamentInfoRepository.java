package senet.server.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;
import com.yazino.platform.repository.tournament.TournamentInfoRepository;

public class FitTournamentInfoRepository implements TournamentInfoRepository {

    @Override
    public TournamentSchedule findTournamentSchedule(final String gameType) {
        throw new UnsupportedOperationException("Unimplemented");
    }
}
