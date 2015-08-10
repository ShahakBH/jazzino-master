package com.yazino.platform.service.tournament;

import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TournamentTableService {
    List<BigDecimal> createTables(int numberOfTables,
                                  String gameType,
                                  BigDecimal templateId,
                                  String clientId,
                                  String partnerId,
                                  String tableName);

    void removeTables(Collection<BigDecimal> tableIds);

    void requestClosing(Collection<BigDecimal> tableIds);

    void reopenAndStartNewGame(BigDecimal tableId,
                               PlayerGroup playerIds,
                               BigDecimal variationTemplateId,
                               String clientId);

    int getOpenTableCount(Collection<BigDecimal> tableIds);

    Set<PlayerAtTableInformation> getActivePlayers(Collection<BigDecimal> tableIds);

    Client findClientById(String clientId);
}
