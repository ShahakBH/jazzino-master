package senet.server.util;

import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.platform.processor.tournament.TournamentPlayerStatisticPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InMemoryTournamentPlayerStatisticPublisher implements TournamentPlayerStatisticPublisher {
	private final List<GameStatistic> allStats = new ArrayList<GameStatistic>();

	public void publishStatistics(BigDecimal playerId, String gameType, Collection<GameStatistic> statistics) {
		allStats.addAll(statistics);
	}

	public List<GameStatistic> getAllStats() {
		return allStats;
	}

	public void clear() {
		allStats.clear();
	}
}
