package senet.server.tournament;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.tournament.TournamentStatus;
import fitlibrary.SetUpFixture;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.math.BigDecimal;
import java.text.ParseException;

import static org.apache.commons.lang3.Validate.notNull;

public class SetupChipsAtEndOfLevel extends SetUpFixture {
    private static final String TEMPLATE_NAME = "Blackjack Tournament";

    private InMemoryPlayerDetailsService playerService;
    private final PlayerRegistersForTournament registration = new PlayerRegistersForTournament();
    private final int level;
    private final TournamentFixture tournamentFixture;

    public SetupChipsAtEndOfLevel(int level, TournamentFixture tournamentFixture,
                                  final InMemoryPlayerDetailsService playerService) throws ParseException {
        this.playerService = playerService;
        this.level = level;
        this.tournamentFixture = tournamentFixture;
        if (level == 1) {
            tournamentFixture.createTournamentFromTemplate(TournamentFixture.TOURNAMENT_DEFAULT_NAME, TEMPLATE_NAME);
            tournamentFixture.forceStateForTo(TournamentFixture.TOURNAMENT_DEFAULT_NAME, TournamentStatus.REGISTERING.name());
        } else {
            tournamentFixture.startLevelForTournament(TournamentFixture.TOURNAMENT_DEFAULT_NAME);
        }
        registration.tournamentName = TournamentFixture.TOURNAMENT_DEFAULT_NAME;
    }

    public void playerChips(String player, String chips) throws ParseException {
        if (level == 1) {
            registration.playerName = player;
            final String result = registration.message();
            if (!"OK".equalsIgnoreCase(result)) {
                throw new RuntimeException("Couldn't register player: " + result);
            }
        }
        if (chips != null && !"".equals(chips)) {
            updateTournamentPlayerBalance(player, chips);
        }
    }

    private void updateTournamentPlayerBalance(String playerName, String amount) {
        BasicProfileInformation player = playerService.findByName(playerName);
        if (player == null) {
            throw new IllegalStateException("Player does not exist: " + playerName);
        }
        notNull(player.getPlayerId(), "Player ID is not available");

        final Tournament tournament = TournamentFixture.getTournamentByName(TournamentFixture.TOURNAMENT_DEFAULT_NAME);
        notNull(tournament, String.format("No tournmanet of name '%s' exists", TournamentFixture.TOURNAMENT_DEFAULT_NAME));

        final TournamentPlayer tournamentPlayer = tournament.findPlayer(player.getPlayerId());
        notNull(tournamentPlayer, "Player is not registered for tournament: " + playerName);

        final BigDecimal accountId = tournamentPlayer.getAccountId();
        final InternalWalletService wallet = tournamentFixture.getTournamentHost().getInternalWalletService();
        final BigDecimal previousBalance;
        try {
            previousBalance = wallet.getBalance(accountId);
        } catch (WalletServiceException e) {
            throw new Error(e);
        }
        try {
            wallet.postTransaction(accountId, new BigDecimal(amount).subtract(previousBalance), "Create Account", "Fitnesse deposit", TransactionContext.EMPTY);
        } catch (WalletServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
