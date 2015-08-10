package strata.server.lobby.api.promotion;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class ProgressiveDailyAwardPromotion extends DailyAwardPromotion {

    private Long promoId;

    public ProgressiveDailyAwardPromotion(final PromotionType promotionType) {
        super(promotionType);
    }

    public ProgressiveDailyAwardPromotion(final PromotionType type,
                                          final Long promoId,
                                          final BigDecimal amount) {
        super(type);
        this.addConfigurationItem(REWARD_CHIPS_KEY, amount.toString());
        this.promoId = promoId;
    }

    public BigDecimal getAmount() {
        return getConfiguration().getConfigurationValueAsBigDecimal(REWARD_CHIPS_KEY);
    }

    public Long getPromoId() {
        return promoId;
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
        final ProgressiveDailyAwardPromotion rhs = (ProgressiveDailyAwardPromotion) obj;
        return new EqualsBuilder()
                .append(promoId, rhs.promoId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(promoId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(promoId)
                .toString();
    }
}
