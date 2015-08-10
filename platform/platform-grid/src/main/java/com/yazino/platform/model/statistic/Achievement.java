package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class Achievement implements Serializable {
    private static final long serialVersionUID = 3175239171937018372L;

    private String id;
    private String title;
    private String message;
    private String shortDescription;
    private String howToGet;
    private String postedAchievementTitleText;
    private String postedAchievementTitleLink;
    private String postedAchievementActionText;
    private String postedAchievementActionLink;
    private Set<String> events;
    private String accumulator;
    private String accumulatorParameters;
    private String gameType;
    private Integer level;
    private boolean recurring;

    public Achievement(final String id,
                       final Integer level,
                       final String title,
                       final String message,
                       final String shortDescription,
                       final String howToGet,
                       final String postedAchievementTitleText,
                       final String postedAchievementTitleLink,
                       final String postedAchievementActionText,
                       final String postedAchievementActionLink,
                       final String gameType,
                       final Set<String> events,
                       final String accumulator,
                       final String accumulatorParameters,
                       final boolean recurring) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.shortDescription = shortDescription;
        this.level = level;
        this.howToGet = howToGet;
        this.postedAchievementTitleLink = postedAchievementTitleLink;
        this.postedAchievementActionText = postedAchievementActionText;
        this.postedAchievementActionLink = postedAchievementActionLink;
        this.gameType = gameType;
        this.postedAchievementTitleText = postedAchievementTitleText;
        this.events = events;
        this.accumulator = accumulator;
        this.accumulatorParameters = accumulatorParameters;
        this.recurring = recurring;
    }

    public boolean accepts(final StatisticEvent... eventsToTest) {
        notNull(eventsToTest, "Events To Test may not be null");

        if (this.events == null || this.events.size() == 0) {
            return eventsToTest.length == 0;
        }

        if (this.events.size() != eventsToTest.length) {
            return false;
        }

        final Set<String> eventsToMatch = new HashSet<String>();
        for (StatisticEvent event : eventsToTest) {
            eventsToMatch.add(event.getEvent());
        }

        return ObjectUtils.equals(this.events, eventsToMatch);
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(final String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public Set<String> getEvents() {
        return events;
    }

    public void setEvents(final Set<String> events) {
        this.events = events;
    }

    public String getAccumulator() {
        return accumulator;
    }

    public void setAccumulator(final String accumulator) {
        this.accumulator = accumulator;
    }

    public String getAccumulatorParameters() {
        return accumulatorParameters;
    }

    public void setAccumulatorParameters(final String accumulatorParameters) {
        this.accumulatorParameters = accumulatorParameters;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getPostedAchievementTitleText() {
        return postedAchievementTitleText;
    }

    public void setPostedAchievementTitleText(final String postedAchievementTitleText) {
        this.postedAchievementTitleText = postedAchievementTitleText;
    }

    public String getPostedAchievementTitleLink() {
        return postedAchievementTitleLink;
    }

    public void setPostedAchievementTitleLink(final String postedAchievementTitleLink) {
        this.postedAchievementTitleLink = postedAchievementTitleLink;
    }

    public String getPostedAchievementActionText() {
        return postedAchievementActionText;
    }

    public void setPostedAchievementActionText(final String postedAchievementActionText) {
        this.postedAchievementActionText = postedAchievementActionText;
    }

    public String getPostedAchievementActionLink() {
        return postedAchievementActionLink;
    }

    public void setPostedAchievementActionLink(final String postedAchievementActionLink) {
        this.postedAchievementActionLink = postedAchievementActionLink;
    }

    public String getHowToGet() {
        return howToGet;
    }

    public void setHowToGet(final String howToGet) {
        this.howToGet = howToGet;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public Boolean getRecurring() {
        return recurring;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(final boolean recurring) {
        this.recurring = recurring;
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
        final Achievement rhs = (Achievement) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
