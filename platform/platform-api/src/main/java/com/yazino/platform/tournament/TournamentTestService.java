package com.yazino.platform.tournament;

import org.openspaces.remoting.Routing;

/**
 * Methods for clients testing services.
 * <p/>
 * Nothing in here should ever be used for production.
 */
public interface TournamentTestService {

    void fakeSummary(@Routing("getTournamentId") Summary tournamentSummary);

}
