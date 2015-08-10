package com.yazino.platform.repository.community;

import com.yazino.platform.model.community.TableInvite;

import java.math.BigDecimal;
import java.util.List;

public interface TableInviteRepository {

    List<TableInvite> findInvitationsByPlayerId(BigDecimal playerId);

    List<TableInvite> removeInvitationsByTableId(BigDecimal tableId);

    /*
      * Writes table invitation to repository and persists it to database. If the player already has
      * an invitation to the table, then the existing invitation is updated
      */
    void invitePlayerToTable(BigDecimal playerId, BigDecimal tableId);

    TableInvite findByTableAndPlayerId(BigDecimal bigDecimal, BigDecimal playerId);
}
