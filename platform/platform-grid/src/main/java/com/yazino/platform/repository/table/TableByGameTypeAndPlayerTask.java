package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TableByGameTypeAndPlayerTask extends ReadWithTemplateTask {

    private final String gameType;
    private final BigDecimal ownerId;

    public TableByGameTypeAndPlayerTask(final String gameType, final BigDecimal ownerId) {
        notNull(gameType, "gameType cannot be null");
        notNull(ownerId, "ownerId cannot be null");
        this.gameType = gameType;
        this.ownerId = ownerId;
    }

    @Override
    Table createTemplate() {
        final Table template = new Table();
        template.setGameTypeId(gameType);
        template.setOwnerId(ownerId);
        template.setTableStatus(TableStatus.open);
        return template;
    }
}
