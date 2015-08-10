package senet.server.table;

import fit.ColumnFixture;

import java.math.BigDecimal;

public class PlayerJoinsTable extends ColumnFixture {

    private final FitTableRepository tableRepository;
    private final BigDecimal player;

    public String table;

    public PlayerJoinsTable(FitTableRepository tableRepository, BigDecimal playerId) {
        this.tableRepository = tableRepository;
        this.player = playerId;
    }

    public void execute() throws Exception {
        super.execute();
        tableRepository.addPlayerToTable(player, new BigDecimal(table));        
    }
}
