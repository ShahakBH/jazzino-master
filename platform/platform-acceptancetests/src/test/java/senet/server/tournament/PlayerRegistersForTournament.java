package senet.server.tournament;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.platform.tournament.TournamentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import senet.server.WiredColumnFixture;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static org.apache.commons.lang3.Validate.notNull;

public class PlayerRegistersForTournament extends WiredColumnFixture {

    @Autowired(required = true)
    @Qualifier("tournamentHost")
    private TournamentHost tournamentHost;

    @Autowired
    private InMemoryPlayerRepository playerRepository;

    @Autowired
    private InMemoryPlayerDetailsService playerDetailsService;

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public String tournamentName;
    public String playerName;
    public String signupTime;

    public String message() throws ParseException {
        notNull(playerName, "Player name was not set");
        notNull(tournamentName, "Tournament name was not set");

        final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();

        if (signupTime != null && signupTime.trim().length() > 0) {
            timeSource.setMillis(dateFormat.parse(signupTime).getTime());
        } else {
            timeSource.addMillis(1000);
        }

        Player player = playerRepository.findByName(playerName);
        if (player == null) {
            System.out.println("Creating player " + playerName);
            player = createPlayer(playerName);
        }
        notNull(player.getPlayerId(), "Player ID is not available");

        final Tournament tournament = TournamentFixture.getTournamentByName(tournamentName);
        notNull(tournament, String.format("No tournament of name '%s' exists", tournamentName));

        try {
            tournament.addPlayer(player, tournamentHost);

        } catch (TournamentException e) {
            switch (e.getResult()) {
                case PLAYER_ALREADY_REGISTERED:
                    return "You are already registered for this tournament";
                case TRANSFER_FAILED:
                    return "You must pay the entry fee before you can join the tournament";
                case MAX_PLAYERS_EXCEEDED:
                    return "This tournament is already full";
                case NO_RESPONSE_RETURNED:
                    return "No response";
                case BEFORE_SIGNUP_TIME:
                    return "This tournament is not open yet";
                case AFTER_SIGNUP_TIME:
                    return "This tournament is now closed";
                case PLAYER_NOT_REGISTERED:
                    return "You are not registered for this tournament";
                case UNKNOWN:
                    return "Unknown error";
                case SUCCESS:
                    return "OK";
                default:
                    return "Unexpected error type: " + e.getResult();
            }
        }

        return "OK";
    }

    private Player createPlayer(String userName) {
        final BasicProfileInformation newPlayer = playerDetailsService.createNewPlayer(userName, "aPicture",
                NON_GUEST, new PaymentPreferences(), new PlayerCreditConfiguration(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        return new Player(newPlayer.getPlayerId(), newPlayer.getName(), newPlayer.getAccountId(), newPlayer.getPictureUrl(), null, null, null);
    }

    public TournamentHost getTournamentHost() {
        return tournamentHost;
    }

    public void setTournamentHost(final TournamentHost tournamentHost) {
        this.tournamentHost = tournamentHost;
    }
}
