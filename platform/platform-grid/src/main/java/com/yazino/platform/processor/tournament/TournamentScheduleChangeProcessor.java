package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentScheduleUpdateRequest;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.platform.tournament.TournamentStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.space.mode.PostPrimary;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.notify.Notify;
import org.openspaces.events.notify.NotifyType;
import org.openspaces.events.notify.ReplicateNotifyTemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class will generate schedule update requests when updates to a {@link Tournament} are made.
 */
@EventDriven
@Notify(gigaSpace = "gigaSpace", replicateNotifyTemplate = ReplicateNotifyTemplateType.FALSE)
@NotifyType(write = true, update = true)
public class TournamentScheduleChangeProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentScheduleChangeProcessor.class);

    private static final Tournament TEMPLATE = new Tournament();

    private final GigaSpace localSpace;
    private final GigaSpace globalSpace;
    private final TournamentRegistrationInfoTransformer transformer;

    @Autowired
    public TournamentScheduleChangeProcessor(@Qualifier("gigaSpace") final GigaSpace localSpace,
                                             @Qualifier("globalGigaSpace") final GigaSpace globalSpace) {
        notNull(localSpace, "localSpace must not be null");
        notNull(globalSpace, "globalSpace must not be null");

        this.globalSpace = globalSpace;
        this.localSpace = localSpace;

        this.transformer = new TournamentRegistrationInfoTransformer();
    }

    @EventTemplate
    public Tournament template() {
        return TEMPLATE;
    }

    @PostPrimary
    public void initialiseTournamentSchedule() throws Exception {
        final Tournament[] tournaments = localSpace.readMultiple(TEMPLATE, Integer.MAX_VALUE);

        LOG.debug("Initialisation tournament schedule for partition; found {} tournaments", tournaments.length);

        final Map<Pair<String, String>, Set<Tournament>> tournamentsBySchedule = splitTournamentsBySchedule(tournaments);
        for (Pair<String, String> scheduleKey : tournamentsBySchedule.keySet()) {
            updateScheduleWith(tournamentsBySchedule.get(scheduleKey), scheduleKey.getKey(), scheduleKey.getValue());
        }
    }

    @SpaceDataEvent
    public void processTournamentChange(final Tournament tournament) {
        LOG.debug("Processing tournament change [{}]", tournament);

        final String gameType = tournament.getTournamentVariationTemplate().getGameType();
        requestScheduleUpdateFor(gameType, tournament.getPartnerId(), tournament);
    }

    private void updateScheduleWith(final Collection<Tournament> tournaments,
                                    final String gameType,
                                    final String partnerId) {
        for (Tournament tournament : tournaments) {
            requestScheduleUpdateFor(gameType, partnerId, tournament);
        }
    }

    private void requestScheduleUpdateFor(final String gameType,
                                          final String partnerId,
                                          final Tournament tournament) {
        final TournamentRegistrationInfo registrationInfo = transformer.apply(tournament);
        final TournamentStatus status = tournament.getTournamentStatus();

        globalSpace.write(new TournamentScheduleUpdateRequest(gameType, partnerId, registrationInfo, status));
    }

    @SuppressWarnings("unchecked")
    private Map<Pair<String, String>, Set<Tournament>> splitTournamentsBySchedule(final Tournament[] tournaments) {
        final Map<Pair<String, String>, Set<Tournament>> tournamentsBySchedule
                = new HashMap<Pair<String, String>, Set<Tournament>>();

        for (Tournament tournament : tournaments) {
            final String gameType = tournament.getTournamentVariationTemplate().getGameType();
            final Pair<String, String> scheduleKey = Pair.of(gameType, tournament.getPartnerId());

            Set<Tournament> tournamentForSchedule = tournamentsBySchedule.get(scheduleKey);
            if (tournamentForSchedule == null) {
                tournamentForSchedule = new HashSet<Tournament>();
                tournamentsBySchedule.put(scheduleKey, tournamentForSchedule);
            }
            tournamentForSchedule.add(tournament);
        }

        return tournamentsBySchedule;
    }
}
