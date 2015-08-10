package strata.server.worker.audit.playedtracking.repository;

import com.yazino.platform.event.message.PlayerPlayedEvent;

import java.math.BigDecimal;

public interface PlayerPlayedEventRepository {

    PlayerPlayedEvent forAccount(BigDecimal accountId);

    void store(BigDecimal accountId, PlayerPlayedEvent event);
}
