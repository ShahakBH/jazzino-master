package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableType;

import static org.apache.commons.lang3.Validate.notNull;

public class TableByTypeTask extends ReadMultipleWithTemplateTask {

    private final TableType tableType;
    private final TableSearchOption[] options;


    public TableByTypeTask(final TableType tableType, final TableSearchOption... options) {
        notNull(tableType, "tableType cannot be null");
        this.tableType = tableType;
        this.options = options;
    }

    @Override
    Table createTemplate() {
        final Table template = new Table();

        switch (tableType) {
            case ALL:
                break;
            case PUBLIC:
                template.setShowInLobby(true);
                break;
            case TOURNAMENT:
                template.setShowInLobby(false);
                template.setHasOwner(false);
                break;
            case PRIVATE:
                template.setHasOwner(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown table type: " + tableType);
        }

        if (options != null) {
            for (TableSearchOption option : options) {
                switch (option) {
                    case IN_ERROR_STATE:
                        template.setTableStatus(TableStatus.error);
                        break;
                    case ONLY_OPEN:
                        template.setOpen(true);
                        break;
                    case ONLY_WITH_PLAYERS:
                        template.setHasPlayers(true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown search option: " + option);
                }
            }
        }
        return template;
    }
}
