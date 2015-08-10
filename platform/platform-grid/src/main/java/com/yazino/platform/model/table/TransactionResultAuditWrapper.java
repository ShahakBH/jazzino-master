package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.game.api.TransactionResult;

import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class TransactionResultAuditWrapper {
    private TransactionResult transactionResult;
    private AuditContext auditContext;
    private BigDecimal tableId;
    private Long gameId;
    private String spaceId;

    public TransactionResultAuditWrapper() {
    }

    public TransactionResultAuditWrapper(final TransactionResult transactionResult,
                                         final AuditContext auditContext,
                                         final BigDecimal tableId,
                                         final Long gameId) {
        this.transactionResult = transactionResult;
        this.auditContext = auditContext;
        this.tableId = tableId;
        this.gameId = gameId;
    }

    public TransactionResult getTransactionResult() {
        return transactionResult;
    }

    public void setTransactionResult(final TransactionResult transactionResult) {
        this.transactionResult = transactionResult;
    }

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setAuditContext(final AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    @SpaceId(autoGenerate = true)
    @SpaceRouting
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(final Long gameId) {
        this.gameId = gameId;
    }
}
