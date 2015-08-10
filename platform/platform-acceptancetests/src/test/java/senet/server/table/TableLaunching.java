package senet.server.table;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import senet.server.WiredDoFixture;

import java.math.BigDecimal;

public class TableLaunching extends WiredDoFixture {

	static final String CLIENT_ID = "Red Blackjack";
	static final String PARTNER_ID = "TEST";
	static final int MAX_PLAYERS = 5;

	@Autowired(required = true)
	@Qualifier("tableRepository")
	private FitTableRepository fitTableRepository;

	public void clearAllTables() {
		fitTableRepository.clear();
	}

	public SetTablesOpenFor setTablesOpenFor(final String gameType) {
		return new SetTablesOpenFor(gameType, fitTableRepository);
	}

	public CheckTablesOpenFor checkTablesOpenFor(final String gameType) {
		return new CheckTablesOpenFor(fitTableRepository, gameType);
	}

    public PlayerJoinsTable playerJoinsTable(BigDecimal playerId) {
        return new PlayerJoinsTable(fitTableRepository, playerId);
    }

    public ReservationExpiresForPlayer reservationExpiresForPlayer(BigDecimal playerId) {
        return new ReservationExpiresForPlayer(fitTableRepository, playerId);
    }

    public BigDecimal findBigDecimal(String key) {
        return new BigDecimal(key);
    }

    public String showBigDecimal(BigDecimal value) {
        return value.toString();
    }
}
