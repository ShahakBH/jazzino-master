package strata.server.lobby.api.promotion;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

/**
 * Configuration items used by the daily award popup.
 */
public class DailyAwardConfig implements Serializable {

    private static final long serialVersionUID = 1763417767614004220L;
    @JsonIgnore
    private Long promotionId;

    private String mainImage;
    private String mainImageLink;
    private String secondaryImage;
    private String secondaryImageLink;
    private String iosImage;
    private String androidImage;

    private Integer rewardChips;
    @JsonIgnore
    private Integer maxRewards;
    private ProgressiveAwardEnum progressiveAward;

    public DailyAwardConfig() {

    }

    public DailyAwardConfig(final PromotionConfiguration promotionConfiguration) {
        notNull(promotionConfiguration, "configuration is null");
        setMainImage(promotionConfiguration.getConfigurationValue(MAIN_IMAGE_KEY));
        setMainImageLink(promotionConfiguration.getConfigurationValue(MAIN_IMAGE_LINK_KEY));
        setSecondaryImage(promotionConfiguration.getConfigurationValue(SECONDARY_IMAGE_KEY));
        setSecondaryImageLink(promotionConfiguration.getConfigurationValue(SECONDARY_IMAGE_LINK_KEY));
        setIosImage(promotionConfiguration.getConfigurationValue(IOS_IMAGE_KEY));
        setAndroidImage(promotionConfiguration.getConfigurationValue(ANDROID_IMAGE_KEY));

        setRewardChips(promotionConfiguration.getConfigurationValueAsInteger(REWARD_CHIPS_KEY));
        setMaxRewards(promotionConfiguration.getConfigurationValueAsInteger(MAX_REWARDS_KEY));
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

    public boolean hasMainImageLink() {
        return StringUtils.isNotBlank(mainImageLink);
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

    public boolean hasSecondaryImageLink() {
        return StringUtils.isNotBlank(secondaryImageLink);
    }

    public void setSecondaryImageLink(final String secondaryImageLink) {
        this.secondaryImageLink = secondaryImageLink;
    }

    public String getIosImage() {
        return iosImage;
    }

    public void setIosImage(final String iosImage) {
        this.iosImage = iosImage;
    }

    public String getAndroidImage() {
        return androidImage;
    }

    public void setAndroidImage(final String androidImage) {
        this.androidImage = androidImage;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
    }

    public Integer getRewardChips() {
        return rewardChips;
    }

    public void setRewardChips(final Integer rewardChips) {
        this.rewardChips = rewardChips;
    }

    public Integer getMaxRewards() {
        return maxRewards;
    }

    public void setMaxRewards(final Integer maxRewards) {
        this.maxRewards = maxRewards;
    }

    public ProgressiveAwardEnum getProgressiveAward() {
        return progressiveAward;
    }

    public void setProgressiveAward(final ProgressiveAwardEnum progressiveAward) {
        this.progressiveAward = progressiveAward;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(17, 37, this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
