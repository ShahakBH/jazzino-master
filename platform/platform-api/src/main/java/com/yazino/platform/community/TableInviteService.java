package com.yazino.platform.community;

import org.openspaces.remoting.Routing;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.List;

public interface TableInviteService {

    List<BigDecimal> findTableInvitesByPlayerId(@Routing BigDecimal playerId);

    void invitePlayerToTable(@Routing BigDecimal playerId,
                             String playerName,
                             BigDecimal tableId,
                             GameType gameType);

    void tableClosed(BigDecimal tableId);

    void sendInvitations(@Routing final BigDecimal playerId, List<TableInviteSummary> allInvites);
}
