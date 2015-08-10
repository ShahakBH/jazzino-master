package com.yazino.web.util;

import com.yazino.web.service.PlayerDetailsLobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.time.SystemTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class PlayerFriendsCache implements Serializable {

    private static final long serialVersionUID = -3758503112819171983L;
    public static final int DEFAULT_LEASE_TIME = 30000;

    private PlayerDetailsLobbyService playerDetailsLobbyService;
    private TimeSource timeSource = new SystemTimeSource();

    private long leaseTime = DEFAULT_LEASE_TIME;
    private BigDecimal playerId;
    private Set<BigDecimal> friendIds;
    private long leaseTimeout;

    /* CGLIB Constructor */
    PlayerFriendsCache() {
    }

    @Autowired
    public PlayerFriendsCache(
            @Qualifier("playerDetailsLobbyService") final PlayerDetailsLobbyService playerDetailsLobbyService) {
        this.playerDetailsLobbyService = playerDetailsLobbyService;
    }

    public Set<BigDecimal> getFriendIds(final BigDecimal forPlayerId) {
        if (friendIds == null
                || leaseTimeout < timeSource.getCurrentTimeStamp()
                || this.playerId.compareTo(forPlayerId) != 0) {
            setFriendIds(forPlayerId);
        }
        return new HashSet<BigDecimal>(friendIds);
    }

    public void setLeaseTime(final long leaseTime) {
        this.leaseTime = leaseTime;
    }

    public void setTimeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    private void setFriendIds(final BigDecimal forPlayerId) {
        friendIds = playerDetailsLobbyService.getFriends(forPlayerId);
        this.playerId = forPlayerId;
        leaseTimeout = timeSource.getCurrentTimeStamp() + leaseTime;
    }
}
