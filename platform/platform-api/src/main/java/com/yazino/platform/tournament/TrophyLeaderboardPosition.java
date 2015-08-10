package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class TrophyLeaderboardPosition implements Serializable {
	private static final long serialVersionUID = -2282760138063045578L;

	private final int position;
	private final long awardPoints;
	private final long awardPayout;
	private final BigDecimal trophyId;

	public TrophyLeaderboardPosition(final int position,
									 final long awardPoints,
									 final long awardPayout) {
		this(position, awardPoints, awardPayout, null);
	}

	public TrophyLeaderboardPosition(final int position,
									 final long awardPoints,
									 final long awardPayout,
									 final BigDecimal trophyId) {
		this.position = position;
		this.awardPoints = awardPoints;
		this.awardPayout = awardPayout;
		this.trophyId = trophyId;
	}

	public int getPosition() {
		return position;
	}

	public long getAwardPoints() {
		return awardPoints;
	}

	public long getAwardPayout() {
		return awardPayout;
	}

	public BigDecimal getTrophyId() {
		return trophyId;
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

		final TrophyLeaderboardPosition rhs = (TrophyLeaderboardPosition) obj;
		return new EqualsBuilder()
				.append(position, rhs.position)
				.append(awardPoints, rhs.awardPoints)
				.append(awardPayout, rhs.awardPayout)
				.append(trophyId, rhs.trophyId)
				.isEquals();
	}


	@Override
	public int hashCode() {
		return new HashCodeBuilder(11, 17)
				.append(position)
				.append(awardPoints)
				.append(awardPayout)
				.append(trophyId)
				.toHashCode();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
