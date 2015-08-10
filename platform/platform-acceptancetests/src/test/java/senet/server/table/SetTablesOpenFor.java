package senet.server.table;

import com.yazino.platform.table.TableStatus;
import fit.ColumnFixture;
import com.yazino.platform.model.table.Table;

import java.math.BigDecimal;

public class SetTablesOpenFor extends ColumnFixture {

    private String gameType;
    private FitTableRepository tableRepository;

    public String stake;
    public String type;
    public int players;
    private BigDecimal tableId;

    public SetTablesOpenFor(final String gameType,
                            final FitTableRepository tableRepository) {
        this.gameType = gameType;
        this.tableRepository = tableRepository;

        tableRepository.clear(gameType);
    }

    public void execute() throws Exception {
        super.execute();
        final Table table = new Table();
        table.setClientId(TableLaunching.CLIENT_ID);
        table.setGameTypeId(gameType);
        table.setTemplateName(type + "-" + stake);
        table.setTableStatus(TableStatus.open);
        table.setFull(players == TableLaunching.MAX_PLAYERS);

        tableRepository.save(table);

        for (int i = 0; i < players; ++i) {
            tableRepository.addPlayerToTable(table.getTableId());
        }

        tableId = table.getTableId();

    }

    public String tableId() {
        return tableId.toString();
    }

}
