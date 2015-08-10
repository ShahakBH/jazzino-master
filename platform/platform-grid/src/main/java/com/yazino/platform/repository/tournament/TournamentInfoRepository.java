package com.yazino.platform.repository.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;

public interface TournamentInfoRepository {

    TournamentSchedule findTournamentSchedule(String gameType);

}

