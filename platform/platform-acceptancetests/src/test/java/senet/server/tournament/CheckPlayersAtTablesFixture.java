package senet.server.tournament;

import com.yazino.platform.community.BasicProfileInformation;
import fit.ColumnFixture;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import senet.server.table.FitTournamentTableService;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class CheckPlayersAtTablesFixture extends ColumnFixture {

    private InMemoryPlayerDetailsService playerService;
    public String tournamentName;
    public String tableIndex;

    public CheckPlayersAtTablesFixture(final InMemoryPlayerDetailsService playerService) {
        this.playerService = playerService;
    }

    public String listPlayers() {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = TournamentFixture.getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final List<BigDecimal> tables = FitTournamentTableService.getTablesForTournament();
        final BigDecimal tableId = tables.get(tableIndex.charAt(0) - 65);

        final List<TournamentPlayer> players = FitTournamentTableService.getPlayersForTable(tableId);
        if (players == null) {
            throw new IllegalArgumentException("Invalid table id " + tableId);
        }
        final StringBuilder output = new StringBuilder();
        for (final TournamentPlayer player : players) {
            if (output.length() > 0) {
                output.append(", ");
            }
            final BasicProfileInformation playerObj = playerService.getBasicProfileInformation(player.getPlayerId());
            if (playerObj == null) {
                throw new IllegalArgumentException("Couldn't find player with ID " + player.getPlayerId());
            }
            output.append(playerObj.getName());
        }

        return output.toString();
    }

}
