package senet.server.tournament;

import com.yazino.platform.Partner;
import com.yazino.platform.model.tournament.TournamentSchedule;
import com.yazino.platform.repository.tournament.TournamentScheduleRepository;

import java.util.HashMap;
import java.util.Map;

public class FitTournamentScheduleRepository implements TournamentScheduleRepository {

    private final Map<String, TournamentSchedule> schedules = new HashMap<String, TournamentSchedule>();

    @Override
    public TournamentSchedule findByGameType(final String gameType) {
        return schedules.get(gameType);
    }

    @Override
    public void save(final TournamentSchedule tournamentSchedule) {
        schedules.put(tournamentSchedule.getGameType(), tournamentSchedule);
    }
}
