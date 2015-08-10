package com.yazino.platform.model.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A group of players for allocation to a table.
 */
public class PlayerGroup implements Iterable<TournamentPlayer>, Serializable {
    private static final long serialVersionUID = -8169247000426473832L;

    private final List<TournamentPlayer> players = new ArrayList<>();

    public PlayerGroup() {
    }

    public PlayerGroup(final Collection<TournamentPlayer> tournamentPlayers) {
        if (tournamentPlayers != null) {
            players.addAll(tournamentPlayers);
        }
    }

    public void add(final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "Tournament Player may not be null");

        players.add(tournamentPlayer);
    }

    public void addAll(final Collection<TournamentPlayer> tournamentPlayers) {
        if (tournamentPlayers != null) {
            players.addAll(tournamentPlayers);
        }
    }

    @Override
    public Iterator<TournamentPlayer> iterator() {
        return players.iterator();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }


    public List<TournamentPlayer> asList() {
        return Collections.unmodifiableList(players);
    }


    public Map<BigDecimal, BigDecimal> asAccountIdList() {
        final Map<BigDecimal, BigDecimal> accounts = new HashMap<>();

        for (final TournamentPlayer player : players) {
            accounts.put(player.getPlayerId(), player.getAccountId());
        }

        return accounts;
    }


    public Collection<PlayerAtTableInformation> asPlayerInformationCollection() {
        final List<PlayerAtTableInformation> playerList = new ArrayList<>();

        for (final TournamentPlayer player : players) {
            final GamePlayer gamePlayer = new GamePlayer(player.getPlayerId(), null, player.getName());
            playerList.add(new PlayerAtTableInformation(gamePlayer, player.getProperties()));
        }

        return playerList;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PlayerGroup rhs = (PlayerGroup) obj;
        return new EqualsBuilder()
                .append(players, rhs.players)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(players)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
