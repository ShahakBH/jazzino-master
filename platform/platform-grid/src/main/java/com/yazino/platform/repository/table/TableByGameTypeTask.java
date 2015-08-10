package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;

import static org.apache.commons.lang3.Validate.notNull;

public class TableByGameTypeTask extends ReadMultipleWithTemplateTask {

    private final String gameTypeId;

    public TableByGameTypeTask(final String gameTypeId) {
        notNull(gameTypeId, "gameTypeId cannot be null");
        this.gameTypeId = gameTypeId;
    }


    @Override
    Table createTemplate() {
        final Table template = new Table();
        template.setGameTypeId(gameTypeId);
        template.setHasPlayers(true);
        return template;
    }
}
