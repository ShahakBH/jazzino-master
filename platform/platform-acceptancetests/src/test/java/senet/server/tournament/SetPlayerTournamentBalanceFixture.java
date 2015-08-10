package senet.server.tournament;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.tournament.TournamentException;
import fitlibrary.SetUpFixture;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class SetPlayerTournamentBalanceFixture extends SetUpFixture {

	private final WalletService walletService;
    private final InMemoryPlayerDetailsService playerService;

    public SetPlayerTournamentBalanceFixture(final WalletService walletService,
                                             final InMemoryPlayerDetailsService playerService) {
		this.walletService = walletService;
        this.playerService = playerService;
    }

	public void tournamentNamePlayerBalance(final String tournamentName,
											final String playerName,
											final int chips)
			throws TournamentException {
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

		final BigDecimal currentAccountBalance;
		try {
			currentAccountBalance = walletService.getBalance(tournamentPlayer.getAccountId());
		} catch (WalletServiceException e) {
			throw new Error(e);
		}
        try {
            walletService.postTransaction(tournamentPlayer.getAccountId(), BigDecimal.ZERO.subtract(currentAccountBalance), "Topup", "Clear balance", TransactionContext.EMPTY);
            walletService.postTransaction(tournamentPlayer.getAccountId(), BigDecimal.valueOf(chips), "Topup", "Reset balance", TransactionContext.EMPTY);
        } catch (WalletServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
