package strata.server.lobby.api.promotion;

import org.joda.time.DateTime;

import java.math.BigDecimal;

public class MobileTopUpResult extends TopUpResult {
    private String imageUrl;

    public MobileTopUpResult(final BigDecimal playerId, final TopUpStatus status, final DateTime lastTopUpDate) {
        super(playerId, status, lastTopUpDate);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
