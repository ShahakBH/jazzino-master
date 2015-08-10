package com.yazino.bi.operations.view;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

public class DailyAwardPopupImagesForm {
    private ImageForm mainImage = new ImageForm();
    private ImageForm secondaryImage = new ImageForm();
    private ImageForm iosImage = new ImageForm();
    private ImageForm androidImage = new ImageForm();

    public DailyAwardPopupImagesForm() {
    }

    public DailyAwardPopupImagesForm(final Promotion promotion) {
        final PromotionConfiguration config = promotion.getConfiguration();
        if (config != null && config.hasConfigItems()) {
            mainImage.setImageUrl(config.getConfigurationValue(MAIN_IMAGE_KEY));
            mainImage.setImageLink(config.getConfigurationValue(MAIN_IMAGE_LINK_KEY));
            if (StringUtils.isNotBlank(mainImage.getImageUrl())) {
                mainImage.setImageType("current");
            } else {
                mainImage.setImageType("default");
            }

            secondaryImage.setImageUrl(config.getConfigurationValue(SECONDARY_IMAGE_KEY));
            secondaryImage.setImageLink(config.getConfigurationValue(SECONDARY_IMAGE_LINK_KEY));
            if (StringUtils.isNotBlank(secondaryImage.getImageUrl())) {
                secondaryImage.setImageType("current");
            } else {
                secondaryImage.setImageType("default");
            }

            iosImage.setImageUrl(config.getConfigurationValue(IOS_IMAGE_KEY));
            if (StringUtils.isNotBlank(iosImage.getImageUrl())) {
                iosImage.setImageType("current");
            } else {
                iosImage.setImageType("default");
            }

            androidImage.setImageUrl(config.getConfigurationValue(ANDROID_IMAGE_KEY));
            if (StringUtils.isNotBlank(androidImage.getImageUrl())) {
                androidImage.setImageType("current");
            } else {
                androidImage.setImageType("default");
            }
        }
    }

    public void addToPromotion(final Promotion promotion) {
        promotion.addConfigurationItem(MAIN_IMAGE_KEY, mainImage.getImageUrl());
        promotion.addConfigurationItem(MAIN_IMAGE_LINK_KEY, mainImage.getImageLink());
        promotion.addConfigurationItem(SECONDARY_IMAGE_KEY, secondaryImage.getImageUrl());
        promotion.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, secondaryImage.getImageLink());
        promotion.addConfigurationItem(IOS_IMAGE_KEY, iosImage.getImageUrl());
        promotion.addConfigurationItem(ANDROID_IMAGE_KEY, androidImage.getImageUrl());
    }


    public ImageForm getMainImage() {
        return mainImage;
    }

    public void setMainImage(final ImageForm mainImage) {
        this.mainImage = mainImage;
    }

    public ImageForm getSecondaryImage() {
        return secondaryImage;
    }

    public void setSecondaryImage(final ImageForm secondaryImage) {
        this.secondaryImage = secondaryImage;
    }

    public ImageForm getIosImage() {
        return iosImage;
    }

    public void setIosImage(final ImageForm iosImage) {
        this.iosImage = iosImage;
    }

    public ImageForm getAndroidImage() {
        return androidImage;
    }

    public void setAndroidImage(final ImageForm androidImage) {
        this.androidImage = androidImage;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
