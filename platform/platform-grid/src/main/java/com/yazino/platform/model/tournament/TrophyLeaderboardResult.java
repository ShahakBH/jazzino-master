package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class TrophyLeaderboardResult implements Serializable {
    private static final long serialVersionUID = 6074783393209199627L;

    private List<TrophyLeaderboardPlayerResult> playerResults;

    private BigDecimal leaderboardId;
    private DateTime resultTime;
    private DateTime expiryTime;

    public TrophyLeaderboardResult() {
    }

    public TrophyLeaderboardResult(final BigDecimal leaderboardId,
                                   final DateTime resultTime) {
        notNull(leaderboardId, "Leaderboard ID may not be null");
        notNull(resultTime, "Result Time may not be null");

        this.leaderboardId = leaderboardId;
        this.resultTime = resultTime;
    }

    public TrophyLeaderboardResult(final BigDecimal leaderboardId,
                                   final DateTime resultTime,
                                   final DateTime expiryTime,
                                   final List<TrophyLeaderboardPlayerResult> playerResults) {
        notNull(leaderboardId, "Leaderboard ID may not be null");
        notNull(resultTime, "Result Time may not be null");
        notNull(expiryTime, "Expiry Time may not be null");
        notNull(playerResults, "Player Result may not be null");

        this.leaderboardId = leaderboardId;
        this.resultTime = resultTime;
        this.expiryTime = expiryTime;

        playerResults().addAll(playerResults);
        sort(playerResults(), TrophyLeaderboardPlayerResult.SORT_BY_POSITION);
    }

    /**
     * This is a replacement for {@link java.util.Collections#sort(java.util.List, java.util.Comparator)} with
     * a minor change to remove reliance on the optional {@link java.util.ListIterator#set(Object)} method.
     * <p/>
     * This will allow its use with {@link java.util.concurrent.CopyOnWriteArrayList}, which the Sun original
     * breaks on.
     *
     * @param list       the list to sort.
     * @param comparator the comparator to sort using.
     */
    @SuppressWarnings("unchecked")
    private <T> void sort(final List<T> list,
                          final Comparator<? super T> comparator) {
        final Object[] listAsArray = list.toArray();
        Arrays.sort(listAsArray, (Comparator) comparator);
        for (int i = 0; i < listAsArray.length; i++) {
            list.set(i, (T) listAsArray[i]);
        }
    }

    private List<TrophyLeaderboardPlayerResult> playerResults() {
        if (playerResults == null) {
            playerResults = new CopyOnWriteArrayList<TrophyLeaderboardPlayerResult>();
        }

        return playerResults;
    }

    public List<TrophyLeaderboardPlayerResult> getPlayerResults() {
        return playerResults;
    }

    public void setPlayerResults(final List<TrophyLeaderboardPlayerResult> playerResults) {
        this.playerResults = playerResults;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getLeaderboardId() {
        return leaderboardId;
    }

    public void setLeaderboardId(final BigDecimal leaderboardId) {
        this.leaderboardId = leaderboardId;
    }

    public DateTime getResultTime() {
        return resultTime;
    }

    public void setResultTime(final DateTime resultTime) {
        this.resultTime = resultTime;
    }

    public DateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(final DateTime expiryTime) {
        this.expiryTime = expiryTime;
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

        final TrophyLeaderboardResult rhs = (TrophyLeaderboardResult) obj;
        return new EqualsBuilder()
                .append(leaderboardId, rhs.leaderboardId)
                .append(resultTime, rhs.resultTime)
                .append(expiryTime, rhs.expiryTime)
                .append(playerResults, rhs.playerResults)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(leaderboardId)
                .append(resultTime)
                .append(expiryTime)
                .append(playerResults)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
