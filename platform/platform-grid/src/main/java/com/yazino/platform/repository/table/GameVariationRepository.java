package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.GameVariation;

import java.math.BigDecimal;
import java.util.Set;

public interface GameVariationRepository {
    void refreshAll();

    GameVariation findById(BigDecimal id);

    BigDecimal getIdForName(String name,
                            String gameType);

    Set<GameVariation> variationsFor(String gameType);

    void loadTemplatesIfRequired();

    void populateProperties(Table table);
}
