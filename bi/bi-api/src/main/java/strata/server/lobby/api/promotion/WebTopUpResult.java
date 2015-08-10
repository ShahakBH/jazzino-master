package strata.server.lobby.api.promotion;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class WebTopUpResult extends TopUpResult {
    private String mainImage;
    private String mainImageLink;
    private String secondaryImage;
    private String secondaryImageLink;
    private List<BigDecimal> promotionValueList;

    public WebTopUpResult(final BigDecimal playerId, final TopUpStatus status, final DateTime lastTopUpDate) {
        super(playerId, status, lastTopUpDate);
    }

    public List<BigDecimal> getPromotionValueList() {
        return promotionValueList;
    }

    public void setPromotionValueList(final List<BigDecimal> promotionValueList) {
        this.promotionValueList = promotionValueList;
    }

    public void setPromotionValueList(final BigDecimal... progressiveAmount) {
        this.promotionValueList = Arrays.asList(progressiveAmount);
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(final String mainImage) {
        this.mainImage = mainImage;
    }

    public String getMainImageLink() {
        return mainImageLink;
    }

    public void setMainImageLink(final String mainImageLink) {
        this.mainImageLink = mainImageLink;
    }

    public String getSecondaryImage() {
        return secondaryImage;
    }

    public void setSecondaryImage(final String secondaryImage) {
        this.secondaryImage = secondaryImage;
    }

    public String getSecondaryImageLink() {
        return secondaryImageLink;
    }

    public void setSecondaryImageLink(final String secondaryImageLink) {
        this.secondaryImageLink = secondaryImageLink;
    }
}
