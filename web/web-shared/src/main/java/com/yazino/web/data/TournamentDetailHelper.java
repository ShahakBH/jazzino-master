package com.yazino.web.data;

import com.yazino.platform.tournament.TournamentDetail;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import java.math.BigDecimal;
import java.util.Set;

public final class TournamentDetailHelper {
    private static final DateTimeZone GMT_ZONE = DateTimeZone.forID("GMT");

    private TournamentDetailHelper() {
        // utility class
    }

    public static TournamentDetail buildTournamentDetail(final String gameType,
                                                         final BigDecimal playerId,
                                                         final Set<BigDecimal> friends,
                                                         final TournamentRegistrationInfo tournamentRegistrationInfo) {
        final Long millisToStart = tournamentRegistrationInfo.getStartTimeStamp().getMillis() - DateTimeUtils.currentTimeMillis();
        final DateTime gmtStartTime = tournamentRegistrationInfo.getStartTimeStamp().withZone(GMT_ZONE);
        return new TournamentDetail(
                tournamentRegistrationInfo.getTournamentId(),
                tournamentRegistrationInfo.getName(),
                gameType,
                tournamentRegistrationInfo.getVariationTemplateName(),
                tournamentRegistrationInfo.getDescription(),
                tournamentRegistrationInfo.getNumberOfPlayers(),
                tournamentRegistrationInfo.countMatchingPlayersRegistered(friends),
                millisToStart <= 0,
                playerId != null && tournamentRegistrationInfo.isRegistered(playerId),
                null, //not required for this list atm
                tournamentRegistrationInfo.getCurrentPrizePool(),
                tournamentRegistrationInfo.getEntryFee(),
                millisToStart,
                gmtStartTime.toDate());

    }
}
