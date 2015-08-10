package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.*;

import java.util.ArrayList;
import java.util.List;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.MAX_REWARDS_KEY;

public abstract class PromotionForm<T extends Promotion> {

    private Long id;
    private PromotionType promotionType;
    private String name;
    private List<Platform> platforms = new ArrayList<>();
    private DateTime startDate;
    private DateTime endDate;
    private Integer startHour;
    private Integer startMinute;
    private Integer endHour;
    private Integer endMinute;
    private String allPlayers;
    private Integer priority;
    private int seed;
    private int controlGroupPercentage;
    private ControlGroupFunctionType controlGroupFunction;
    private Integer maximumRewards;
    private DailyAwardPopupImagesForm dailyAwardPopupImagesForm;

    public PromotionForm() {
        dailyAwardPopupImagesForm = new DailyAwardPopupImagesForm();
    }

    public PromotionForm(final T promotion) {
        id = promotion.getId();
        promotionType = promotion.getPromotionType();
        name = promotion.getName();
        platforms = promotion.getPlatforms();
        startDate = promotion.getStartDate();
        endDate = promotion.getEndDate();
        startHour = promotion.getStartDate().getHourOfDay();
        startMinute = promotion.getStartDate().getMinuteOfHour();
        endHour = promotion.getEndDate().getHourOfDay();
        endMinute = promotion.getEndDate().getMinuteOfHour();
        if (promotion.isAllPlayers()) {
            allPlayers = "ALL";
        } else {
            allPlayers = "SELECTED";
        }
        priority = promotion.getPriority();
        seed = promotion.getSeed();
        controlGroupPercentage = promotion.getControlGroupPercentage();
        controlGroupFunction = promotion.getControlGroupFunction();

        final PromotionConfiguration config = promotion.getConfiguration();
        if (config != null && config.hasConfigItems()) {
            dailyAwardPopupImagesForm = new DailyAwardPopupImagesForm(promotion);
            maximumRewards = config.getConfigurationValueAsInteger(MAX_REWARDS_KEY);
        } else {
            dailyAwardPopupImagesForm = new DailyAwardPopupImagesForm();
        }
    }

    @SuppressWarnings("unchecked")
    public T buildPromotion() {
        final T promotion = (T) PromotionFactory.createPromotion(promotionType);
        if (dailyAwardPopupImagesForm != null) {
            dailyAwardPopupImagesForm.addToPromotion(promotion);
        }
        promotion.setId(getId());
        promotion.setName(getName());
        promotion.setPlatforms(getPlatforms());
        promotion.setStartDate(getStartDate().withMinuteOfHour(getStartMinute()).withHourOfDay(getStartHour()));
        promotion.setEndDate(getEndDate().withMinuteOfHour(getEndMinute()).withHourOfDay(getEndHour()));
        promotion.setAllPlayers(!"SELECTED".equals(getAllPlayers()));
        promotion.setPriority(getPriority());
        promotion.setSeed(seed);
        promotion.setControlGroupPercentage(controlGroupPercentage);
        promotion.setControlGroupFunction(controlGroupFunction);
        return promotion;
    }

    public boolean isDefault() {
        return promotionType.getDefaultPromotionName().equals(name);
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(final List<Platform> platforms) {
        this.platforms = platforms;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(final DateTime startDate) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(final DateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getStartHour() {
        return startHour;
    }

    public void setStartHour(final Integer startHour) {
        this.startHour = startHour;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(final Integer startMinute) {
        this.startMinute = startMinute;
    }

    public Integer getEndHour() {
        return endHour;
    }

    public void setEndHour(final Integer endHour) {
        this.endHour = endHour;
    }

    public Integer getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(final Integer endMinute) {
        this.endMinute = endMinute;
    }

    public String getAllPlayers() {
        return allPlayers;
    }

    public void setAllPlayers(final String allPlayers) {
        this.allPlayers = allPlayers;
    }

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(final PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(final int seed) {
        this.seed = seed;
    }

    public int getControlGroupPercentage() {
        return controlGroupPercentage;
    }

    public void setControlGroupPercentage(final int controlGroupPercentage) {
        this.controlGroupPercentage = controlGroupPercentage;
    }

    public ControlGroupFunctionType getControlGroupFunction() {
        return controlGroupFunction;
    }

    public void setControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        this.controlGroupFunction = controlGroupFunction;
    }

    public Integer getMaximumRewards() {
        return maximumRewards;
    }

    public void setMaximumRewards(final Integer maximumRewards) {
        this.maximumRewards = maximumRewards;
    }

    public ImageForm getMainImage() {
        return dailyAwardPopupImagesForm.getMainImage();
    }

    public void setMainImage(final ImageForm mainImage) {
        dailyAwardPopupImagesForm.setMainImage(mainImage);
    }

    public ImageForm getSecondaryImage() {
        return dailyAwardPopupImagesForm.getSecondaryImage();
    }

    public void setSecondaryImage(final ImageForm secondaryImage) {
        dailyAwardPopupImagesForm.setSecondaryImage(secondaryImage);
    }

    public ImageForm getIosImage() {
        return dailyAwardPopupImagesForm.getIosImage();
    }

    public void setIosImage(final ImageForm iosImage) {
        dailyAwardPopupImagesForm.setIosImage(iosImage);
    }

    public ImageForm getAndroidImage()  {
        return dailyAwardPopupImagesForm.getAndroidImage();
    }

    public void setAndroidImage(final ImageForm androidImage)  {
        dailyAwardPopupImagesForm.setAndroidImage(androidImage);
    }

    public DailyAwardPopupImagesForm getDailyAwardPopupImagesForm() {
        return dailyAwardPopupImagesForm;
    }

    public void setDailyAwardPopupImagesForm(final DailyAwardPopupImagesForm dailyAwardPopupImagesForm) {
        this.dailyAwardPopupImagesForm = dailyAwardPopupImagesForm;
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

