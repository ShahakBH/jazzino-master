package senet.server.tournament;

import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import fit.ColumnFixture;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class PlayerTournamentBalanceFixture extends ColumnFixture {

    private final InMemoryPlayerDetailsService playerService;
    private final WalletService walletService;

    public String tournamentName;
    public String playerName;

    public PlayerTournamentBalanceFixture(final WalletService walletService,
                                          final InMemoryPlayerDetailsService playerService) {
		this.walletService = walletService;
        this.playerService = playerService;
    }

	public String balance() {
		notBlank(tournamentName, "Tournament name must be set");
		notBlank(playerName, "Player name must be set");

		BasicProfileInformation player = playerService.findByName(playerName);
		if (player == null) {
			throw new IllegalStateException("Player does not exist: " + playerName);
		}
		notNull(player.getPlayerId(), "Player ID is not available");

		final Tournament tournament = TournamentFixture.getTournamentByName(tournamentName);
		notNull(tournament, String.format("No tournmanet of name '%s' exists", tournamentName));

		final TournamentPlayer tournamentPlayer = tournament.findPlayer(player.getPlayerId());
		notNull(tournamentPlayer, "Player is not registered for tournament: " + playerName);

		try {
			return walletService.getBalance(tournamentPlayer.getAccountId()).setScale(2).toPlainString();
		} catch (WalletServiceException e) {
			throw new Error(e);
		}
	}

}
