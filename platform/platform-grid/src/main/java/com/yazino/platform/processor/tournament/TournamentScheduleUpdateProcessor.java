package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;
import com.yazino.platform.model.tournament.TournamentScheduleUpdateRequest;
import com.yazino.platform.repository.tournament.TournamentScheduleRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class TournamentScheduleUpdateProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentScheduleUpdateProcessor.class);

    private static final TournamentScheduleUpdateRequest TEMPLATE = new TournamentScheduleUpdateRequest();

    private final TournamentScheduleRepository tournamentScheduleRepository;

    @Autowired
    public TournamentScheduleUpdateProcessor(final TournamentScheduleRepository tournamentScheduleRepository) {
        notNull(tournamentScheduleRepository, "tournamentScheduleRepository may not be null");

        this.tournamentScheduleRepository = tournamentScheduleRepository;
    }

    @EventTemplate
    public TournamentScheduleUpdateRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TournamentScheduleUpdateRequest updateRequest) {
        LOG.debug("Processing schedule update [{}]", updateRequest);
        if (updateRequest == null) {
            return;
        }

        TournamentSchedule schedule = findSchedule(updateRequest.getGameType());

        switch (updateRequest.getStatus()) {
            case REGISTERING:
                schedule.addRegistrationInfo(updateRequest.getRegistrationInfo());
                break;

            case WAITING_FOR_CLIENTS:
            case ON_BREAK:
            case RUNNING:
                schedule.addInProgressTournament(updateRequest.getRegistrationInfo());
                break;

            default:
                schedule.removeRegistrationInfo(updateRequest.getRegistrationInfo().getTournamentId());
                break;
        }

        tournamentScheduleRepository.save(schedule);
    }

    private TournamentSchedule findSchedule(final String gameType) {
        TournamentSchedule schedule = tournamentScheduleRepository.findByGameType(gameType);

        if (schedule == null) {
            schedule = new TournamentSchedule();
            schedule.setGameType(gameType);
        }
        return schedule;
    }
}
