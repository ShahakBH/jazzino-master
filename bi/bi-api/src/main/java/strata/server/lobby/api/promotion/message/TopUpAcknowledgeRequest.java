package strata.server.lobby.api.promotion.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopUpAcknowledgeRequest implements PromotionMessage {

    private static final long serialVersionUID = 1L;

    private BigDecimal playerId;
    private DateTime topUpDate;

    public TopUpAcknowledgeRequest() {
    }

    public TopUpAcknowledgeRequest(final BigDecimal playerId, final DateTime topUpDate) {
        this.playerId = playerId;
        this.topUpDate = topUpDate;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public PromotionMessageType getMessageType() {
        return PromotionMessageType.ACKNOWLEDGEMENT;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public DateTime getTopUpDate() {
        return topUpDate;
    }

    public void setTopUpDate(final DateTime topUpDate) {
        this.topUpDate = topUpDate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final TopUpAcknowledgeRequest rhs = (TopUpAcknowledgeRequest) obj;
        return new EqualsBuilder()
                .append(topUpDate, rhs.topUpDate)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(topUpDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(playerId)
                .append(topUpDate)
                .toString();
    }

    public boolean isInvalid() {
        return playerId == null || topUpDate == null;
    }
}
