package com.yazino.platform.event.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TableEvent implements PlatformEvent, Serializable {
    private static final long serialVersionUID = -3748912884684292172L;

    @JsonProperty("id")
    private BigDecimal tableId;
    @JsonProperty("gmTyp")
    private String gameTypeId;
    @JsonProperty("tmpNm")
    private String templateName;
    @JsonProperty("tid")
    private BigDecimal templateId;

    private TableEvent() {
    }

    public TableEvent(final BigDecimal tableId,
                      final String gameTypeId,
                      final BigDecimal templateId,
                      final String templateName) {
        this.tableId = tableId;
        this.gameTypeId = gameTypeId;
        this.templateName = templateName;
        this.templateId = templateId;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.TABLE;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public String getGameTypeId() {
        return gameTypeId;
    }

    public String getTemplateName() {
        return templateName;
    }

    private void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    private void setGameTypeId(final String gameTypeId) {
        this.gameTypeId = gameTypeId;
    }

    private void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    private void setTemplateId(final BigDecimal templateId) {
        this.templateId = templateId;
    }

    public BigDecimal getTemplateId() {
        return templateId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TableEvent rhs = (TableEvent) obj;
        return new EqualsBuilder()
                .append(this.gameTypeId, rhs.gameTypeId)
                .append(this.templateName, rhs.templateName)
                .append(this.templateId, rhs.templateId)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(gameTypeId)
                .append(templateName)
                .append(templateId)
                .toHashCode();
    }
}
