package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TableByOwnershipTask extends ReadMultipleWithTemplateTask {

    private final BigDecimal playerId;

    public TableByOwnershipTask(final BigDecimal playerId) {
        notNull(playerId, "playerId cannot be null");
        this.playerId = playerId;
    }


    @Override
    Table createTemplate() {
        final Table template = new Table();
        template.setOwnerId(playerId);
        template.setTableStatus(TableStatus.open);
        return template;
    }
}
