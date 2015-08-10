package com.yazino.platform.community;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A collection of trophies won by a particular player, i.e. their cabinet!
 */
public class TrophyCabinet implements Serializable {
	private static final long serialVersionUID = 8574415032521462370L;

	private final Map<String, TrophySummary> trophySummaries = new HashMap<String, TrophySummary>();

	private String gameType = "UNKNOWN";
	private String playerName = "UNKNOWN";

	public String getGameType() {
		return gameType;
	}

	public void setGameType(final String gameType) {
		notNull(gameType, "gameType must not be null");
		this.gameType = gameType;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(final String playerName) {
		notNull(playerName, "playerName must not be null");
		this.playerName = playerName;
	}

	public Map<String, TrophySummary> getTrophySummaries() {
		return Collections.unmodifiableMap(trophySummaries);
	}

	public boolean hasTrophySummary(final String trophyName) {
		notNull(trophyName, "trophyName must not be null");
		return trophySummaries.containsKey(trophyName);
	}

	public TrophySummary getTrophySummary(final String trophyName) {
		notNull(trophyName, "trophyName must not be null");
		return trophySummaries.get(trophyName);
	}

	public void addTrophySummary(final TrophySummary trophySummary) {
		notNull(trophySummary, "playerTrophy must not be null");
		trophySummaries.put(trophySummary.getName(), trophySummary);
	}

	public int getTotalTrophyCount() {
		int total = 0;
		for (TrophySummary summary : trophySummaries.values()) {
			total += summary.getCount();
		}
		return total;

	}

	public int getTotalTrophies(final Collection<String> names) {
		int total = 0;
		for (String name : names) {
			final TrophySummary summary = getTrophySummary(name);
			if (summary != null) {
				total += summary.getCount();
			}
		}
		return total;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
