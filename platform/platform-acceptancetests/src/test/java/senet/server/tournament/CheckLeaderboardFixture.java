package senet.server.tournament;

import com.yazino.platform.tournament.TournamentStatus;
import fitlibrary.SetFixture;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CheckLeaderboardFixture extends SetFixture {

	public CheckLeaderboardFixture(TournamentFixture tournamentFixture) {
		final Tournament tournament = TournamentFixture.getTournamentByName(TournamentFixture.TOURNAMENT_DEFAULT_NAME);
		if (tournament.getTournamentStatus() == TournamentStatus.REGISTERING) {
			tournamentFixture.startLevelForTournament(TournamentFixture.TOURNAMENT_DEFAULT_NAME);
		}
		tournamentFixture.stopLevelForTournament(TournamentFixture.TOURNAMENT_DEFAULT_NAME);
		final Set<TournamentPlayer> players = tournament.tournamentPlayers();
		final List<LeaderboardItem> collection = new ArrayList<LeaderboardItem>();
		for (TournamentPlayer player : players) {
			collection.add(new LeaderboardItem(player.getName(), player.getLeaderboardPosition()));
		}
		Collections.sort(collection);
		super.setActualCollection(collection);
	}

	public class LeaderboardItem implements Comparable<LeaderboardItem> {
		public Integer rank;
		public String player;

		public LeaderboardItem(String player, Integer rank) {
			this.player = player;
			this.rank = rank;
		}

		public int compareTo(LeaderboardItem leaderboardItem) {
			if (rank != null && leaderboardItem != null) {
				return rank.compareTo(leaderboardItem.rank);
			}
			return 0;
		}
	}
}
