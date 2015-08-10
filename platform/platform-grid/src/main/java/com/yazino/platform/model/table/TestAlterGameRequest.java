package com.yazino.platform.model.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.GameStatus;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A request to update the GameStatus for a table.
 * <p/>
 * This is intended for use by test methods only.
 */
public class TestAlterGameRequest implements TableRequest, Serializable {
    private static final long serialVersionUID = -7637294732147647728L;

    private final TableRequestType requestType = TableRequestType.TEST_ALTER_GAME;

    private final BigDecimal tableId;
    private final GameStatus gameStatus;

    public TestAlterGameRequest(final BigDecimal tableId,
                                final GameStatus gameStatus) {
        notNull(tableId, "Table ID may not be null");
        notNull(gameStatus, "Game Status may not be null");

        this.tableId = tableId;
        this.gameStatus = gameStatus;
    }

    @Override
    public BigDecimal getTableId() {
        return tableId;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }


    @Override
    public TableRequestType getRequestType() {
        return requestType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TestAlterGameRequest rhs = (TestAlterGameRequest) obj;
        return new EqualsBuilder()
                .append(gameStatus, rhs.gameStatus)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameStatus)
                .append(BigDecimals.strip(tableId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
