package com.yazino.test.statistic;

public class SimplifiedAchievementDefinition {

    public enum Type {
        SINGLE_EVENT("singleEvent"),
        COUNTING_EVENT("thresholdEvent"),
        EVENTS_IN_A_ROW("resettingThresholdEvent");

        private final String accumulator;

        Type(final String accumulator) {
            this.accumulator = accumulator;
        }

        public String getAccumulator() {
            return accumulator;
        }
    }

    private final String achievementId;
    private final Type type;
    private final int threshold;
    private final String[] events;

    public SimplifiedAchievementDefinition(final String achievementId,
                                           final Type type,
                                           final int threshold,
                                           final String... events) {
        this.achievementId = achievementId;
        this.type = type;
        this.threshold = threshold;
        this.events = events;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public Type getType() {
        return type;
    }

    public int getThreshold() {
        return threshold;
    }

    public String[] getEvents() {
        return events;
    }
}
