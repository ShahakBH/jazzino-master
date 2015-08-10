package com.yazino.platform.test;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.util.Visitor;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryTableInfoRepository implements TableDAO {

    private long nextId = 0;
    private final Map<BigDecimal, Table> map = new HashMap<BigDecimal, Table>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<BigDecimal, DateTime> activityTSMap = new HashMap<BigDecimal, DateTime>();

    public InMemoryTableInfoRepository() {
    }

    public int size() {
        return map.size();
    }

    public Collection<Table> values() {
        return map.values();
    }

    public Table findById(final BigDecimal id) {
        return map.get(id);
    }

    public BigDecimal createTableInfo(final Table ti) {
        ti.setTableId(BigDecimal.valueOf(nextId++));
        map.put(ti.getTableId(), ti);
        final DateTime ts = new DateTime();
        ti.setCreatedDateTime(ts);
        activityTSMap.put(ti.getTableId(), ts);
        return ti.getTableId();
    }

    public boolean save(final Table table) {
        if (table == null) {
            return false;
        }
        boolean newTable = map.containsKey(table.getTableId());
        map.put(table.getTableId(), table);
        activityTSMap.put(table.getTableId(), new DateTime());
        return newTable;
    }

    private Map<UUID, BigDecimal> tableCreationRequestMap = new HashMap<UUID, BigDecimal>();

    public Map<UUID, BigDecimal> getTableCreationRequestMap() {
        return this.tableCreationRequestMap;
    }

    public void logTableCreationRequestResults(final UUID creationRequestId,
                                               final BigDecimal tableId,
                                               final String message) {
        tableCreationRequestMap.put(creationRequestId, tableId);
    }

    public Map<String, String> loadVaritionProperties(
            final BigDecimal variationTemplateId,
            final boolean onlyPublished) {
        return new HashMap<String, String>();
    }

    public void clear() {
        map.clear();
    }

    public void visitTables(final TableStatus status,
                            final Visitor<Table> visitor) {
        for (Table table : map.values()) {
            if (status.equals(table.getTableStatus())) {
                visitor.visit(table);
            }
        }
    }

    public long getNextId() {
        return nextId;
    }

    public void setNextId(final long nextId) {
        this.nextId = nextId;
    }
}
