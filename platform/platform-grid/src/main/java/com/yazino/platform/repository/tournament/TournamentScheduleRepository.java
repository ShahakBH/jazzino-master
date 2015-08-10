package com.yazino.platform.repository.tournament;


import com.yazino.platform.model.tournament.TournamentSchedule;

public interface TournamentScheduleRepository {

    TournamentSchedule findByGameType(String gameType);

    void save(TournamentSchedule tournamentSchedule);

}
