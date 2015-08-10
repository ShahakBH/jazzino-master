package strata.server.lobby.api.promotion;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

public class BuyChipsPromotion extends Promotion {

    public static final String PAYMENT_METHODS_KEY = "payment.methods";
    public static final String IN_GAME_NOTIFICATION_MSG_KEY = "ingame.notification.msg";
    public static final String IN_GAME_NOTIFICATION_HEADER_KEY = "ingame.notification.header";

    public static final String CHIP_AMOUNT_IDENTIFIER = ".chips.package.";
    public static final String CHIP_AMOUNT_FORMAT_KEY = "%s" + CHIP_AMOUNT_IDENTIFIER + "%s";

    public static final String ROLLOVER_HEADER_KEY = "rollover.header";
    public static final String ROLLOVER_TEXT_KEY = "rollover.text";

    public BuyChipsPromotion() {
        super(PromotionType.BUY_CHIPS);
    }

    public void configureChipsForPlatformAndPackage(Platform platform, BigDecimal defaultChipAmount, BigDecimal overrideChipAmount) {
        if (!defaultChipAmount.equals(overrideChipAmount)) {
            this.getConfiguration().overrideChipAmountForPlatformAndPackage(platform, defaultChipAmount, overrideChipAmount);
        }
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
        BuyChipsPromotion rhs = (BuyChipsPromotion) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.PAYMENT_METHODS_KEY, rhs.PAYMENT_METHODS_KEY)
                .append(this.IN_GAME_NOTIFICATION_MSG_KEY, rhs.IN_GAME_NOTIFICATION_MSG_KEY)
                .append(this.IN_GAME_NOTIFICATION_HEADER_KEY, rhs.IN_GAME_NOTIFICATION_HEADER_KEY)
                .append(this.CHIP_AMOUNT_IDENTIFIER, rhs.CHIP_AMOUNT_IDENTIFIER)
                .append(this.CHIP_AMOUNT_FORMAT_KEY, rhs.CHIP_AMOUNT_FORMAT_KEY)
                .append(this.ROLLOVER_HEADER_KEY, rhs.ROLLOVER_HEADER_KEY)
                .append(this.ROLLOVER_TEXT_KEY, rhs.ROLLOVER_TEXT_KEY)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(PAYMENT_METHODS_KEY)
                .append(IN_GAME_NOTIFICATION_MSG_KEY)
                .append(IN_GAME_NOTIFICATION_HEADER_KEY)
                .append(CHIP_AMOUNT_IDENTIFIER)
                .append(CHIP_AMOUNT_FORMAT_KEY)
                .append(ROLLOVER_HEADER_KEY)
                .append(ROLLOVER_TEXT_KEY)
                .toHashCode();
    }
}
