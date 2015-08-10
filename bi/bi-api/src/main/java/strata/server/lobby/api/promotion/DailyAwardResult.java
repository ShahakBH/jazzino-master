package strata.server.lobby.api.promotion;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * WEB-3190
 * @deprecated for WEB, once all mobile clients are moved across to do server side topups should move over to TopUpResult
 */
public class DailyAwardResult implements Serializable {
    private static final long serialVersionUID = -7947491863508436802L;

    private BigDecimal topupAmount;
    private BigDecimal balance;
    private int consecutiveDaysPlayed;
    private DailyAwardConfig dailyAwardConfig;
    private List<BigDecimal> promotionValueList;

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }

    public DailyAwardConfig getDailyAwardConfig() {
        return dailyAwardConfig;
    }

    public void setDailyAwardConfig(final DailyAwardConfig dailyAwardConfig) {
        this.dailyAwardConfig = dailyAwardConfig;
    }

    public BigDecimal getTopupAmount() {
        return topupAmount;
    }

    public void setTopupAmount(final BigDecimal topupAmount) {
        this.topupAmount = topupAmount;
    }

    public int getConsecutiveDaysPlayed() {
        return consecutiveDaysPlayed;
    }

    public void setConsecutiveDaysPlayed(final int consecutiveDaysPlayed) {
        this.consecutiveDaysPlayed = consecutiveDaysPlayed;
    }

    public List<BigDecimal> getPromotionValueList() {
        return promotionValueList;
    }

    public void setPromotionValueList(final List<BigDecimal> promotionValueList) {
        this.promotionValueList = promotionValueList;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(17, 37, this);
    }

}
