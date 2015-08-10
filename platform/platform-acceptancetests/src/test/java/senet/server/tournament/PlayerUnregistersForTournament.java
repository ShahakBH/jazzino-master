package senet.server.tournament;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.platform.tournament.TournamentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import senet.server.WiredColumnFixture;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerUnregistersForTournament extends WiredColumnFixture {

    @Autowired(required = true)
    @Qualifier("tournamentHost")
    private TournamentHost tournamentHost;

    @Autowired
    private InMemoryPlayerDetailsService playerService;
    @Autowired
    private InMemoryPlayerRepository playerRepository;

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public String tournamentName;
    public String playerName;
    public String signupTime;
    public boolean feePaid = true;

    public String message() throws ParseException {
        notNull(playerName, "Player name was not set");
        notNull(tournamentName, "Tournament name was not set");

        final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
        final Tournament tournament = TournamentFixture.getTournamentByName(tournamentName);

        if (signupTime != null && signupTime.trim().length() > 0) {
            timeSource.setMillis(dateFormat.parse(signupTime).getTime());
        } else {
            timeSource.setMillis(tournament.getSignupStartTimeStamp().getMillis() + 1);
        }

        Player player = playerRepository.findByName(playerName);
        if (player == null) {
            player = createPlayer(playerName);
        }
        notNull(player.getPlayerId(), "Player ID is not available");

        notNull(tournament, String.format("No tournmanet of name '%s' exists", tournamentName));

        try {
            tournament.removePlayer(player, tournamentHost);

        } catch (TournamentException e) {
            switch (e.getResult()) {
                case PLAYER_ALREADY_REGISTERED:
                    return "You are already registered for this tournament";
                case TRANSFER_FAILED:
                    return "Fees not refunded";
                case MAX_PLAYERS_EXCEEDED:
                    return "This tournament is already full";
                case NO_RESPONSE_RETURNED:
                    return "No response";
                case BEFORE_SIGNUP_TIME:
                    return "This tournament is not open yet";
                case AFTER_SIGNUP_TIME:
                    return "It is too late unregister for this tournament";
                case PLAYER_NOT_REGISTERED:
                    return "You are not registered for this tournament";
                case UNKNOWN:
                    return "Unknown error";
                default:
                    return "Unexpected error type: " + e.getResult();
            }
        }

        return "OK";
    }

    private Player createPlayer(String userName) {
        final BasicProfileInformation newPlayer = new BasicProfileInformation(
                null, userName, "aUserProfileId", null);
        playerService.save(newPlayer);
        return new Player(newPlayer.getPlayerId(), newPlayer.getName(), newPlayer.getAccountId(), newPlayer.getPictureUrl(), null, null, null);
    }

    public TournamentHost getTournamentHost() {
        return tournamentHost;
    }

    public void setTournamentHost(final TournamentHost tournamentHost) {
        this.tournamentHost = tournamentHost;
    }
}
