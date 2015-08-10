package senet.server.table;

import fit.ColumnFixture;

import java.math.BigDecimal;

public class ReservationExpiresForPlayer extends ColumnFixture {

    private final FitTableRepository tableRepository;
    private final BigDecimal playerId;

    public String table;

    public ReservationExpiresForPlayer(FitTableRepository tableRepository, BigDecimal playerId) {
        this.tableRepository = tableRepository;
        this.playerId = playerId;
    }

    public void execute() throws Exception {
        super.execute();
        tableRepository.removeReservation(playerId, new BigDecimal(table));
    }


}
