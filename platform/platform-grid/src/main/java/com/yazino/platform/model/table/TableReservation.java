package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This class represents a reservation at a table.
 * It should be inserted into a space with a time lease (the time taken between clicking 'play now' and 'join' + some
 * buffer).
 */
@SpaceClass
public class TableReservation implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(TableReservation.class);

    private String id;
    private BigDecimal tableId;
    private BigDecimal playerId;

    public TableReservation() {
    }

    public TableReservation(final BigDecimal tableId,
                            final BigDecimal playerId) {
        this.tableId = tableId;
        this.playerId = playerId;
    }

    @SpaceRouting
    @SpaceIndex
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }
}
