package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@SpaceClass(replicate = false)
public class ClosedGameAuditWrapper {

    private String spaceId;
    private BigDecimal tableId;
    private Long gameId;
    private Long increment;
    private String observableStatusXml;
    private String internalStatusXml;
    private AuditContext auditContext;
    private Set<BigDecimal> playerIds;

    public ClosedGameAuditWrapper() {
    }

    public ClosedGameAuditWrapper(final BigDecimal tableId,
                                  final Long gameId,
                                  final Long increment,
                                  final String observableStatusXml,
                                  final String internalStatusXml,
                                  final Set<BigDecimal> playerIds,
                                  final AuditContext auditContext) {
        this.tableId = tableId;
        this.gameId = gameId;
        this.increment = increment;
        this.observableStatusXml = observableStatusXml;
        this.internalStatusXml = internalStatusXml;
        this.auditContext = auditContext;
        this.playerIds = new HashSet<BigDecimal>(playerIds);
    }

    @SpaceId(autoGenerate = true)
    @SpaceRouting
    public String getSpaceId() {
        return spaceId;
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

    public Long getIncrement() {
        return increment;
    }

    public void setIncrement(final Long increment) {
        this.increment = increment;
    }

    public String getObservableStatusXml() {
        return observableStatusXml;
    }

    public void setObservableStatusXml(final String observableStatusXml) {
        this.observableStatusXml = observableStatusXml;
    }

    public String getInternalStatusXml() {
        return internalStatusXml;
    }

    public void setInternalStatusXml(final String internalStatusXml) {
        this.internalStatusXml = internalStatusXml;
    }

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setAuditContext(final AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public Set<BigDecimal> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(final Set<BigDecimal> playerIds) {
        this.playerIds = playerIds;
    }
}
