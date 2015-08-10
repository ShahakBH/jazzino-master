package senet.server.tournament;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.tournament.*;
import fit.Fixture;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import senet.server.WiredDoFixture;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import com.yazino.platform.test.InMemoryPlayerRepository;
import senet.server.table.FitTournamentTableService;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Root tournament do fixture.
 */
public class TournamentFixture extends WiredDoFixture {

    private static final String DEFAULT_PARTNER_ID = "TEST";

    private static final String SYMBOL_TEMPLATES = "TournamentTemplates";
    private static final String SYMBOL_TOURNAMENTS = "Tournaments";
    private static final String SYMBOL_TOURNAMENT_IDS = "TournamentsIds";
    private static final String DEFAULT_CLIENT_ID = "Blue Blackjack";
    public static final String TOURNAMENT_DEFAULT_NAME = "Default Tournament";

    private static final AtomicLong ID_SOURCE = new AtomicLong(0);

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private TournamentService tournamentService;

    @Autowired(required = true)
    @Qualifier("walletService")
    private WalletService walletService;

    @Autowired(required = true)
    @Qualifier("tournamentHost")
    private TournamentHost tournamentHost;

    @Autowired
    private InMemoryPlayerDetailsService playerService;
    @Autowired
    private InMemoryPlayerRepository playerRepository;

    @Autowired
    @Qualifier("playerSessionRepository")
    private PlayerSessionRepository playerSessionRepository;

    public Fixture setupTournamentPlayersForTournament() {
        return new SetupTournamentPlayersFixture(tournamentHost, walletService, playerService, playerRepository);
    }

    public Fixture checkPlayersAtTablesForTournament() {
        return new CheckPlayersAtTablesFixture(playerService);
    }

    public Fixture setPlayerTournamentBalance() {
        return new SetPlayerTournamentBalanceFixture(walletService, playerService);
    }

    public Fixture playerTournamentBalance() {
        return new PlayerTournamentBalanceFixture(walletService, playerService);
    }

    public String[] playersIn(final String tournamentName) {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final List<String> playerNames = new ArrayList<String>();

        for (final TournamentPlayer tournamentPlayer : tournament.tournamentPlayers()) {
            if (tournamentPlayer.getStatus() == TournamentPlayerStatus.ACTIVE) {
                playerNames.add(playerService.getBasicProfileInformation(tournamentPlayer.getPlayerId()).getName());
            }
        }

        Collections.sort(playerNames);

        return playerNames.toArray(new String[playerNames.size()]);
    }

    public void closeSignupFor(final String tournamentName) throws TournamentException {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
//		timeSource.setMillis(System.currentTimeMillis());

        tournament.setSignupEndTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() - 1));
        tournament.setStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() + 1));

        tournament.processEvent(tournamentHost);

        if (tournament.getTournamentStatus() == TournamentStatus.CANCELLING) {
            tournament.cancel(tournamentHost);
        }
    }

    public void startTournament(final String tournamentName) {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        if (tournament.getTournamentStatus() == TournamentStatus.REGISTERING
                || tournament.getTournamentStatus() == TournamentStatus.ANNOUNCED) {
            if (tournament.getStartTimeStamp() == null) {
                tournament.setStartTimeStamp(new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp()));
            }
            SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
            timeSource.setMillis(tournament.getStartTimeStamp().getMillis() + 1);
            tournament.start(tournamentHost);
        } else {
            throw new IllegalStateException("Invalid tournament state: " + tournament.getTournamentStatus());
        }
    }

    public void startLevelForTournament(final String tournamentName) {
        notNull(tournamentName, "Tournament name is not null");
        final Tournament tournament = getTournamentByName(tournamentName);
        System.out.println("STATUS Before START++++ " + tournament.getTournamentStatus());
        notNull(tournament, "No tournament exists with name " + tournamentName);
        if (tournament.getTournamentStatus() == TournamentStatus.ON_BREAK) {
            tournament.startRound(tournamentHost);
        } else if (tournament.getTournamentStatus() == TournamentStatus.REGISTERING
                || tournament.getTournamentStatus() == TournamentStatus.ANNOUNCED) {
            if (tournament.getStartTimeStamp() == null) {
                tournament.setStartTimeStamp(new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp()));
            }
            final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
            timeSource.setMillis(tournament.getStartTimeStamp().getMillis() + 1);
            tournament.start(tournamentHost);
        } else {
            throw new IllegalStateException("Invalid tournament state: " + tournament.getTournamentStatus());
        }
        System.out.println("STATUS AFTER START++++ " + tournament.getTournamentStatus());
        ((SettableTimeSource) tournamentHost.getTimeSource()).addMillis(5);
    }

    public void startLevel() {
        startLevelForTournament(TOURNAMENT_DEFAULT_NAME);
    }

    public void stopLevel() {
        stopLevelForTournament(TOURNAMENT_DEFAULT_NAME);
    }

    public void stopLevelForTournament(final String tournamentName) {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);
        tournament.updateLeaderboard(tournamentHost);

        final TournamentStatus status = tournament.getTournamentStatus();
        System.out.println("STATUSBEFORE STOP++++ " + status);
        if (status == TournamentStatus.RUNNING || status == TournamentStatus.WAITING_FOR_CLIENTS) {
            if (status == TournamentStatus.RUNNING) {
                tournament.finishRound(tournamentHost);
            }
            tournament.setTournamentStatus(TournamentStatus.ON_BREAK);
            long startOfNextRound = 0;
            if (tournament.getStartTimeStamp() != null) {
                startOfNextRound = tournament.getStartTimeStamp().getMillis();
            }

            for (int i = 0; i <= tournament.getCurrentRoundIndex(); ++i) {
                final TournamentVariationRound round = tournament.getTournamentVariationTemplate().getTournamentRounds().get(i);
                startOfNextRound += round.getRoundLength() + round.getRoundEndInterval();
            }

            final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
            timeSource.setMillis(startOfNextRound - 1);
            tournament.processEvent(tournamentHost);

            System.err.println("Stopped, status is " + status);
        } else {
            throw new IllegalStateException("Invalid tournament state: " + status);
        }
        System.out.println("STATUSAFTER STOP++++ " + status);
    }

    public boolean checkTournamentStatusForIs(final String tournamentName, final String status) {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final TournamentStatus tournamentStatus = TournamentStatus.valueOf(status.toUpperCase().replace(' ', '_'));

        System.err.println("Current status is " + tournament.getTournamentStatus() + ", checking for " + tournamentStatus);
        return tournament.getTournamentStatus() == tournamentStatus;
    }

    public boolean tournamentTemplateGotLevelsAdded(final String templateName, final long numberOfRounds) {
        notNull(templateName, "Tournament template may not be null");

        final TournamentVariationTemplate template = getTemplateByName(templateName);
        notNull(template, String.format("Template with name '%s' does not exist", templateName));

        return template.getTournamentRounds().size() == numberOfRounds;
    }

    public boolean tournamentTemplateGotPayoutRanksAdded(final String templateName, final long numberOfPayoutRanks) {
        notNull(templateName, "Tournament template may not be null");

        final TournamentVariationTemplate template = getTemplateByName(templateName);
        notNull(template, String.format("Template with name '%s' does not exist", templateName));

        return template.getTournamentPayouts().size() == numberOfPayoutRanks;
    }

    public boolean totalPayoutPrizeForTournamentTemplateIsPercent(final String templateName, final String totalPrize) {
        notNull(templateName, "Tournament template may not be null");
        notNull(totalPrize, "Total prize percentage template may not be null");

        final TournamentVariationTemplate template = getTemplateByName(templateName);
        notNull(template, String.format("Template with name '%s' does not exist", templateName));

        return calculatePayoutPrizeTotal(template).equals(new BigDecimal(totalPrize + ".0000"));
    }

    private BigDecimal calculatePayoutPrizeTotal(final TournamentVariationTemplate template) {
        BigDecimal prizeTotalPercent = BigDecimal.ZERO;
        for (TournamentVariationPayout tournamentVariationPayout : template.getTournamentPayouts()) {
            prizeTotalPercent = prizeTotalPercent.add(tournamentVariationPayout.getPayout());
        }
        return convertQuotientToPercent(prizeTotalPercent).setScale(4, BigDecimal.ROUND_UP);
    }

    private BigDecimal convertQuotientToPercent(BigDecimal payoutPrizeQuotient) {
        return payoutPrizeQuotient.multiply(BigDecimal.valueOf(100L));
    }

    public void createTournamentTemplateLevelForTemplate(int level, final String templateName) {
        createTournamentTemplateLevelForTemplateWithMinStake(level, templateName, null);
    }

    public void createTournamentTemplateLevelForTemplateWithMinStake(int level, final String templateName, final Integer minStake) {
        notNull(templateName, "Template Name may not be null");

        final TournamentVariationTemplate template = getTemplateByName(templateName);
        notNull(template, String.format("Template with name '%s' does not exist", templateName));

        BigDecimal minimumBalance = BigDecimal.ZERO;
        if (minStake != null) {
            minimumBalance = BigDecimal.valueOf(minStake);
        }

        final TournamentVariationRound round = new TournamentVariationRound(
                level, 60, 300, BigDecimal.valueOf(0), DEFAULT_CLIENT_ID, minimumBalance, minimumBalance.toString());

        List<TournamentVariationRound> rounds = new ArrayList<TournamentVariationRound>(template.getTournamentRounds());
        rounds.add(round);

        final TournamentVariationTemplate updatedTemplate = new TournamentVariationTemplate(
                template.getTournamentVariationTemplateId(),
                template.getTournamentType(),
                template.getTemplateName(),
                template.getEntryFee(),
                template.getServiceFee(),
                template.getPrizePool(),
                template.getStartingChips(),
                template.getMinPlayers(),
                template.getMaxPlayers(),
                template.getGameType(),
                86400000,
                "EVEN_BY_BALANCE",
                null,
                rounds);
        saveTemplate(updatedTemplate);
    }

    public void processTablePlayerRemovalFor(final String tournamentName) {
        notNull(tournamentName, "Tournament name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        tournament.updateLeaderboard(tournamentHost);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final TournamentVariationRound currentRound
                = tournament.getTournamentVariationTemplate().getTournamentRounds().get(tournament.getCurrentRoundIndex());
        if (currentRound == null) {
            return;
        }

        if (currentRound.getMinimumBalance() == null
                || currentRound.getMinimumBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        final Set<TournamentPlayer> players = tournament.tournamentPlayers();
        for (final TournamentPlayer player : players) {
            if (player.getStatus() != TournamentPlayerStatus.ACTIVE) {
                continue;
            }

            final BigDecimal currentBalance;
            try {
                currentBalance = tournamentHost.getInternalWalletService().getBalance(player.getAccountId());
            } catch (WalletServiceException e) {
                throw new Error(e);
            }
            System.err.println("Curret blaance is " + currentBalance + ", min = " + currentRound.getMinimumBalance());
            if (currentBalance == null || currentBalance.compareTo(currentRound.getMinimumBalance()) < 0) {
                System.err.println("Remove player " + player);
                FitTournamentTableService.removePlayer(player);
            }
        }

        final SettableTimeSource timeSource = (SettableTimeSource) tournamentHost.getTimeSource();
        timeSource.setMillis(System.currentTimeMillis());

        if (tournament.getStartTimeStamp() == null) {
            tournament.setStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() - 1));
        }

        tournament.updateLeaderboard(tournamentHost);
        tournament.processEvent(tournamentHost);

        if (tournament.getTournamentStatus() == TournamentStatus.WAITING_FOR_CLIENTS) { // if we're shutting down, finish the job
            tournament.setTournamentStatus(TournamentStatus.ON_BREAK);
            tournament.finish(tournamentHost);
        }
    }

    public void setBalanceForTo(final String playerName, final String balance) throws WalletServiceException {
        notNull(playerName, "Player name is not null");

        BasicProfileInformation player = playerService.findByName(playerName);
        if (player == null) {
            System.out.println("Creating player " + playerName);
            player = createPlayer(playerName);
        }

        System.out.println("Adding " + balance + " to account " + player.getAccountId());

        final BigDecimal currentBalance = walletService.getBalance(player.getAccountId());

        walletService.postTransaction(player.getAccountId(), new BigDecimal(balance).subtract(currentBalance),
                "Create Account", "Fitnesse deposit", TransactionContext.EMPTY);
    }

    public void forceStateForTo(final String tournamentName, final String state) {
        notNull(tournamentName, "Tournament name is not null");
        notNull(state, "State name is not null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "No tournament exists with name " + tournamentName);

        final TournamentStatus status = TournamentStatus.valueOf(state.toUpperCase().replace(' ', '_'));

        // Note: this bypasses logic
        tournament.setTournamentStatus(status);
    }

    public String balanceFor(final String playerName) {
        notNull(playerName, "Player name is not null");

        BasicProfileInformation player = playerService.findByName(playerName);
        if (player == null) {
            System.out.println("Creating player " + playerName);
            player = createPlayer(playerName);
        }

        try {
            return walletService.getBalance(player.getAccountId()).setScale(2).toString();
        } catch (WalletServiceException e) {
            throw new Error(e);
        }
    }

    public BigDecimal getCreditLimit(BigDecimal accountId) {
        return (BigDecimal) jdbcTemplate.queryForObject("SELECT credit_limit FROM ACCOUNT WHERE ACCOUNT_ID=?",
                new Object[]{accountId}, BigDecimal.class);
    }

    public String potBalanceFor(final String tournamentName) {
        notBlank(tournamentName, "Tournament name may not be null");

        final Tournament tournament = getTournamentByName(tournamentName);
        notNull(tournament, "Tournament not found: " + tournamentName);
        return tournament.getPot().setScale(2).toString();
    }

    public void createTournamentFromTemplate(final String tournamentName,
                                             final String templateName)
            throws ParseException {
        createTournamentFromTemplateWithSignupStartTimeAndSignupEndTime(tournamentName, templateName, null, null);
    }

    public void createTournamentFromTemplateWithSignupStartTimeAndSignupEndTime(final String tournamentName,
                                                                                final String templateName,
                                                                                final String signupStartTime,
                                                                                final String signupEndTime)
            throws ParseException {
        notNull(tournamentName, "Tournament Name may not be null");
        notNull(templateName, "Template Name may not be null");

        final TournamentVariationTemplate template = getTemplateByName(templateName);
        notNull(template, String.format("Template with name '%s' does not exist", templateName));

        FitTournamentTableService.clearTournaments();

        final DateTime startTimeStamp = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp()).plusMinutes(5);
        final TournamentDefinition tournamentDef = new TournamentDefinition(
                null,
                tournamentName,
                template,
                signupStartTime != null ? new DateTime(dateFormat.parse(signupStartTime).getTime()) : new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp()).minusSeconds(1),
                signupEndTime != null ? new DateTime(dateFormat.parse(signupEndTime).getTime()) : startTimeStamp,
                startTimeStamp,
                TournamentStatus.ANNOUNCED,
                "aPartner",
                "aFixtureTournament");

        System.err.println("Creating tournament " + tournamentDef);

        try {
            final BigDecimal tournamentId = tournamentService.createTournament(tournamentDef);
            final Tournament tournament = new Tournament(tournamentDef);
            tournament.setTournamentId(tournamentId);
            saveTournament(tournament);
        } catch (TournamentException e) {
            throw new RuntimeException(e);
        }
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TournamentService getTournamentService() {
        return tournamentService;
    }

    public void setTournamentService(final TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    public WalletService getWalletService() {
        return walletService;
    }

    public void setWalletService(final WalletService walletService) {
        this.walletService = walletService;
    }

    public TournamentHost getTournamentHost() {
        return tournamentHost;
    }

    public void setTournamentHost(final TournamentHost tournamentHost) {
        this.tournamentHost = tournamentHost;
    }

    public Fixture chipsAtEndOfLevel(int level) throws ParseException {
        return new SetupChipsAtEndOfLevel(level, this, playerService);
    }

    public Fixture getCheckLeaderboard() {
        return new CheckLeaderboardFixture(this);
    }

    public void playerGoesOffline(String player) {
        ((FitPlayerSessionRepository) playerSessionRepository).removeSession(playerService.findByName(player).getPlayerId());
    }

    private BasicProfileInformation createPlayer(String userName) {
        return playerService.createNewPlayer(
                userName, "aPitcure", NON_GUEST, new PaymentPreferences(),
                new PlayerCreditConfiguration(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public static long getRuntimeUniqueId() {
        return ID_SOURCE.getAndIncrement();
    }

    public static void saveTournament(final Tournament tournament) {
        if (tournament.getTournamentId() == null) {
            throw new IllegalArgumentException("Tournaments should have an ID at save time");
        }
        Map<String, Tournament> tournamentsByName = getMapFromSymbol(SYMBOL_TOURNAMENTS);
        tournamentsByName.put(tournament.getName(), tournament);
        Map<BigDecimal, Tournament> tournamentsById = getMapFromSymbol(SYMBOL_TOURNAMENT_IDS);
        tournamentsById.put(tournament.getTournamentId(), tournament);
    }

    public static Tournament getTournamentByName(final String tournamentName) {
        final Map<String, Tournament> tournaments = getMapFromSymbol(SYMBOL_TOURNAMENTS);
        return tournaments.get(tournamentName);
    }

    static Tournament getTournamentById(final BigDecimal tournamentId) {
        final Map<BigDecimal, Tournament> tournaments = getMapFromSymbol(SYMBOL_TOURNAMENT_IDS);
        return tournaments.get(tournamentId);
    }

    static void saveTemplate(final TournamentVariationTemplate template) {
        final Map<String, TournamentVariationTemplate> templates = getMapFromSymbol(SYMBOL_TEMPLATES);
        templates.put(template.getTemplateName(), template);
    }

    static TournamentVariationTemplate getTemplateByName(final String templateName) {
        final Map<String, TournamentVariationTemplate> templates = getMapFromSymbol(SYMBOL_TEMPLATES);
        return templates.get(templateName);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getMapFromSymbol(final String symbolName) {
        Map<K, V> map = (Map<K, V>) getSymbol(symbolName);
        if (map == null) {
            map = new HashMap<K, V>();
            setSymbol(symbolName, map);
        }
        return map;
    }
}
