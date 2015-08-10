package senet.server.table;

import fitlibrary.SetFixture;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.TableRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class CheckTablesOpenFor extends SetFixture {

    private final FitTableRepository tableRepository;

	public CheckTablesOpenFor(final FitTableRepository tableRepository,
							  final String gameType) {
		final Set<TableWrapper> wrappers = new HashSet<TableWrapper>();

		for (Table tableInfo : tableRepository.getByGameType(gameType)) {
			wrappers.add(new TableWrapper(tableInfo));
		}

		setActualCollection(wrappers);
        this.tableRepository = tableRepository;
	}

	public class TableWrapper {
		private String stake;
		private String type;
		private int players;
        private BigDecimal tableId; 

		public TableWrapper(final Table table) {
			this.players = table.numberOfPlayers(new FitTableRepository.DummyGameRules());
			this.type = table.getTemplateName().split("-")[0];
			this.stake = table.getTemplateName().split("-")[1];
            this.tableId = table.getTableId();
		}

		public String getStake() {
			return stake;
		}

		public String getType() {
			return type;
		}

		public int getPlayers() {
			return players;
		}

    }
}
