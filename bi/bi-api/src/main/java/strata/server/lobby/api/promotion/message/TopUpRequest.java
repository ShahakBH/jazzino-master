package strata.server.lobby.api.promotion.message;

import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopUpRequest implements PromotionMessage {
    private static final long serialVersionUID = 1L;

    private BigDecimal playerId;
    private BigDecimal sessionId;
    private Platform platform;
    private DateTime requestDate;

    public TopUpRequest() {
    }

    public TopUpRequest(final BigDecimal playerId,
                        final Platform platform,
                        final DateTime requestDate,
                        final BigDecimal sessionId) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.platform = platform;
        this.requestDate = requestDate;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public PromotionMessageType getMessageType() {
        return PromotionMessageType.TOPUP_REQUEST;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setPlatform(final Platform platform) {
        this.platform = platform;
    }

    public DateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(final DateTime requestDate) {
        this.requestDate = requestDate;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
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
        final TopUpRequest rhs = (TopUpRequest) obj;
        return new EqualsBuilder()
                .append(platform, rhs.platform)
                .append(requestDate, rhs.requestDate)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .append(platform)
                .append(requestDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(playerId)
                .append(sessionId)
                .append(platform)
                .append(requestDate)
                .toString();
    }

    public boolean isInvalid() {
        return playerId == null || platform == null || requestDate == null;
    }
}
