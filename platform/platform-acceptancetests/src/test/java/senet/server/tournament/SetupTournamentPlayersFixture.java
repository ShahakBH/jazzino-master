package senet.server.tournament;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.platform.tournament.TournamentException;
import fitlibrary.SetUpFixture;

import java.math.BigDecimal;

import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class SetupTournamentPlayersFixture extends SetUpFixture {

    private InMemoryPlayerDetailsService playerService;
    private InMemoryPlayerRepository playerRepository;
    private final TournamentHost tournamentHost;
    private final WalletService walletService;

    public SetupTournamentPlayersFixture(final TournamentHost tournamentHost,
                                         final WalletService walletService,
                                         final InMemoryPlayerDetailsService playerService,
                                         final InMemoryPlayerRepository playerRepository) {
        this.tournamentHost = tournamentHost;
        this.walletService = walletService;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
    }

    public void tournamentNamePlayersTournamentChips(final String tournamentName,
                                                     final String playerName,
                                                     final int chips)
            throws TournamentException, WalletServiceException {
        notBlank(tournamentName, "Tournament name must be set");
        notBlank(playerName, "Player name must be set");

        final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
        timeSource.setMillis(System.currentTimeMillis());

        Player player = playerRepository.findByName(playerName);
        if (player == null) {
            System.out.println("Creating player " + playerName);
            player = createPlayer(playerName);
        }
        notNull(player.getPlayerId(), "Player ID is not available");

        final Tournament tournament = TournamentFixture.getTournamentByName(tournamentName);
        notNull(tournament, String.format("No tournmanet of name '%s' exists", tournamentName));
        timeSource.setMillis(tournament.getSignupStartTimeStamp().getMillis() + 1);
        tournament.addPlayer(player, tournamentHost);

        walletService.postTransaction(tournament.findPlayer(player.getPlayerId()).getAccountId(),
                BigDecimal.valueOf(chips), "Topup", "Test chips set", TransactionContext.EMPTY);
    }

    private Player createPlayer(String userName) {
        final BasicProfileInformation newPlayer = playerService.createNewPlayer(
                userName, "aPitcure", NON_GUEST, new PaymentPreferences(),
                new PlayerCreditConfiguration(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        return new Player(newPlayer.getPlayerId(), newPlayer.getName(), newPlayer.getAccountId(), newPlayer.getPictureUrl(), null, null, null);
    }
}
