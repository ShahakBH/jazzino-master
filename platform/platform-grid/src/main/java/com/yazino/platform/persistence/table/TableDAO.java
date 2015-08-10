package com.yazino.platform.persistence.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.util.Visitor;

import java.math.BigDecimal;

public interface TableDAO {
    Table findById(BigDecimal id);

    boolean save(Table ti);

    void visitTables(TableStatus status, Visitor<Table> visitor);

}
