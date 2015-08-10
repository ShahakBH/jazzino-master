package senet.server.tournament.payout;

import com.yazino.platform.tournament.TournamentException;
import fitlibrary.SetUpFixture;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupRanksFixture extends SetUpFixture {

	private Map<String, TournamentPlayer> nameToPlayer = new HashMap<String, TournamentPlayer>();
	private List<TournamentPlayer> players = new ArrayList<TournamentPlayer>();

	private BigDecimal currentChipValue;
	private int currentRank;
	private int currentRankPosition;
	private int currentPlayerId;

	public void playerEndingChips(String player, int chips) throws TournamentException {
		currentPlayerId++;
		currentRankPosition++;
		final TournamentPlayer tournamentPlayer = new TournamentPlayer(new BigDecimal(currentPlayerId), player);
        tournamentPlayer.setName(player);
		final BigDecimal chipValue = new BigDecimal(chips);
		if (currentChipValue == null || chipValue.compareTo(currentChipValue) < 0) {
			currentRank = currentRankPosition;
		}
		currentChipValue = chipValue;
		tournamentPlayer.setLeaderboardPosition(currentRank);
		nameToPlayer.put(player, tournamentPlayer);
		players.add(tournamentPlayer);
	}

	public List<TournamentPlayer> getPlayers() {
		return players;
	}

	public Map<String, TournamentPlayer> getPlayerNames() {
		return nameToPlayer;
	}
}
