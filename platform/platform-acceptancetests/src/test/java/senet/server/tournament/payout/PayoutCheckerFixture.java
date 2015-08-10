package senet.server.tournament.payout;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.processor.tournament.TournamentPayoutCalculator;
import com.yazino.platform.model.tournament.TournamentPlayer;
import fit.ColumnFixture;

import static org.apache.commons.lang3.Validate.notNull;

public class PayoutCheckerFixture extends ColumnFixture {

    private Tournament tournament;
    private Map < TournamentPlayer, BigDecimal > result;
    public String Player;

    public PayoutCheckerFixture(final Tournament tournament) {
        this.tournament = tournament;
        final TournamentPayoutCalculator calculator = tournament.getPayoutCalculator();
        result =
                calculator
                        .calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());
    }

    public String Payout() {
        final TournamentPlayer player = findPlayerByName(Player);
        final BigDecimal payout = findNumberOfChipsPaidTo(player);
        return toPayoutInChipsString(payout);
    }

    private String toPayoutInChipsString(final BigDecimal payoutInChips) {
        final NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMinimumFractionDigits(0);
        return format.format(payoutInChips.doubleValue());
    }

    private BigDecimal findNumberOfChipsPaidTo(final TournamentPlayer player) {
        final BigDecimal chipsPaidOut = result.get(player);
        return chipsPaidOut == null ? BigDecimal.ZERO : chipsPaidOut;
    }

    private TournamentPlayer findPlayerByName(final String name) {
        notNull(name, "player name may not be null");
        for (final TournamentPlayer candidate : tournament.tournamentPlayers()) {
            if (name.equals(candidate.getName())) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No such player: \"" + name + "\"");
    }
}
