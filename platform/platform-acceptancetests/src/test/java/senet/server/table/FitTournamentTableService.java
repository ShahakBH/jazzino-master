package senet.server.table;


import com.yazino.platform.model.table.Client;
import com.yazino.platform.table.TableStatus;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.service.tournament.TournamentTableService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FitTournamentTableService implements TournamentTableService {

    private static final AtomicLong ID_SOURCE = new AtomicLong();
    private static final List<BigDecimal> TOURNAMENTS_TABLES
            = Collections.synchronizedList(new ArrayList<BigDecimal>());
    private static final Map<BigDecimal, List<TournamentPlayer>> TABLES_PLAYERS
            = Collections.synchronizedMap(new HashMap<BigDecimal, List<TournamentPlayer>>());
    private static final Map<BigDecimal, TableStatus> TABLES_STATUSES
            = Collections.synchronizedMap(new HashMap<BigDecimal, TableStatus>());

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public Client findClientById(final String clientId) {
        final Client client = clientRepository.findById(clientId);
        if (client != null) {
            return new Client(client.getClientId(), client.getNumberOfSeats(), client.getClientFile(),
                    client.getGameType(), client.getClientProperties());
        }
        return null;
    }

    public List<BigDecimal> createTables(
            int numberOfTables,
            String gameType,
            BigDecimal templateId,
            String clientId,
            String partnerId,
            String tableName) {
        final List<BigDecimal> tableIds = new ArrayList<>();

        for (int i = 0; i < numberOfTables; ++i) {
            final BigDecimal newTableId = BigDecimal.valueOf(ID_SOURCE.getAndIncrement());
            tableIds.add(newTableId);
            TABLES_PLAYERS.put(newTableId, new ArrayList<TournamentPlayer>());
            TABLES_STATUSES.put(newTableId, TableStatus.closed);
        }

        return tableIds;
    }

    @Override
    public void removeTables(final Collection<BigDecimal> tableIds) {
        for (final BigDecimal tableId : tableIds) {
            TABLES_STATUSES.remove(tableId);
            TABLES_PLAYERS.remove(tableId);
        }
    }

    public void requestClosing(final Collection<BigDecimal> tableIds) {
        for (final BigDecimal tableId : tableIds) {
            if (!TABLES_STATUSES.containsKey(tableId)) {
                throw new IllegalStateException("Table " + tableId + " does not exist");
            }
            TABLES_STATUSES.put(tableId, TableStatus.closed);
        }
    }

    public void reopenAndStartNewGame(final BigDecimal tableId,
                                      final PlayerGroup players,
                                      final BigDecimal variationTemplateId, String clientId) {
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Must have > 0 players");
        }

        if (!TABLES_PLAYERS.containsKey(tableId)) {
            throw new IllegalStateException("Table " + tableId + " does not exist");
        }
        TABLES_PLAYERS.put(tableId, new ArrayList<>(players.asList()));
        TABLES_STATUSES.put(tableId, TableStatus.open);

        TOURNAMENTS_TABLES.add(tableId);
    }

    public int getOpenTableCount(final Collection<BigDecimal> tableIds) {
        int count = 0;

        for (final BigDecimal tableId : tableIds) {
            if (!TABLES_STATUSES.containsKey(tableId)) {
                throw new IllegalStateException("Table " + tableId + " does not exist");
            }
            if (TABLES_STATUSES.get(tableId) == TableStatus.open) {
                ++count;
            }
        }

        return count;
    }


    public Set<PlayerAtTableInformation> getActivePlayers(final Collection<BigDecimal> tableIds) {
        Set<PlayerAtTableInformation> allPlayers = new HashSet<>();
        for (final BigDecimal tableId : tableIds) {
            final Set<BigDecimal> playerIds = getIds(TABLES_PLAYERS.get(tableId));
            for (final BigDecimal playerId : playerIds) {
                allPlayers.add(new PlayerAtTableInformation(new GamePlayer(playerId, null, "player" + playerId), Collections.<String, String>emptyMap()));
            }
        }
        return allPlayers;
    }

    Set<BigDecimal> getIds(List<TournamentPlayer> players) {
        Set<BigDecimal> ids = new HashSet<>();
        for (TournamentPlayer p : players) {
            ids.add(p.getPlayerId());
        }
        return ids;
    }

    public static void removePlayer(final TournamentPlayer tournamentPlayer) {
        for (final List<TournamentPlayer> players : TABLES_PLAYERS.values()) {
            players.remove(tournamentPlayer);
        }
    }

    public static void clear() {
        TABLES_PLAYERS.clear();
        TABLES_STATUSES.clear();
    }

    public static List<BigDecimal> getTablesForTournament() {
        return TOURNAMENTS_TABLES;
    }

    public static List<TournamentPlayer> getPlayersForTable(final BigDecimal tableId) {
        return TABLES_PLAYERS.get(tableId);
    }

    public static TableStatus getStatusForTable(final BigDecimal tableId) {
        return TABLES_STATUSES.get(tableId);
    }

    public static void clearTournaments() {
        TOURNAMENTS_TABLES.clear();
    }
}
