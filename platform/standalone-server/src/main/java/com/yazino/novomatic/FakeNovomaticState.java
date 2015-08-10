package com.yazino.novomatic;

import com.yazino.novomatic.cgs.NovomaticEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakeNovomaticState implements Serializable {
    private final long balance;
    private final long lines;
    private final long stake;
    private transient final List<NovomaticEvent> events;
    private final FakeEngine currentEngine;
    private final String gamblerHistory;
    private final long gamblerStep;
    private final long gamblerWinMeter;

    public FakeNovomaticState(List<NovomaticEvent> events,
                              FakeEngine currentEngine,
                              long balance,
                              long lines,
                              long stake,
                              String gamblerHistory,
                              long gamblerStep,
                              long gamblerWinMeter) {
        this.currentEngine = currentEngine;
        this.balance = balance;
        this.lines = lines;
        this.stake = stake;
        this.events = events;
        this.gamblerHistory = gamblerHistory;
        this.gamblerStep = gamblerStep;
        this.gamblerWinMeter = gamblerWinMeter;
    }

    public long getBalance() {
        return balance;
    }

    public long getLines() {
        return lines;
    }

    public long getStake() {
        return stake;
    }

    public List<NovomaticEvent> getEvents() {
        return events;
    }

    public FakeEngineRules resolveEngine() {
        return currentEngine.getEngine();
    }

    public String getGamblerHistory() {
        return gamblerHistory;
    }

    public long getGamblerStep() {
        return gamblerStep;
    }

    public long getGamblerWinMeter() {
        return gamblerWinMeter;
    }

    public static class Builder {
        private long balance;
        private long lines;
        private long stake;
        private List<NovomaticEvent> events = new ArrayList<>();
        private FakeEngine currentEngine;
        private String gamblerHistory;
        private long gamblerStep;
        private long gamblerWinMeter;

        public Builder() {
        }

        public Builder(FakeNovomaticState gameState) {
            balance = gameState.balance;
            lines = gameState.lines;
            stake = gameState.stake;
            currentEngine = gameState.currentEngine;
            gamblerHistory = gameState.gamblerHistory;
            gamblerStep = gameState.gamblerStep;
            gamblerWinMeter = gameState.gamblerWinMeter;
        }

        public Builder setBalance(long balance) {
            this.balance = balance;
            return this;
        }

        public Builder setLines(long lines) {
            this.lines = lines;
            return this;
        }

        public Builder setStake(long stake) {
            this.stake = stake;
            return this;
        }

        public Builder setEngine(FakeEngine engine) {
            this.currentEngine = engine;
            return this;
        }

        public Builder addEvents(NovomaticEvent... events) {
            this.events.addAll(Arrays.asList(events));
            return this;
        }

        public Builder setGamblerHistory(String gamblerHistory) {
            this.gamblerHistory = gamblerHistory;
            return this;
        }

        public Builder setGamblerStep(long gamblerStep) {
            this.gamblerStep = gamblerStep;
            return this;
        }

        public Builder setGamblerWinMeter(long gamblerWinMeter) {
            this.gamblerWinMeter = gamblerWinMeter;
            return this;
        }

        public FakeNovomaticState build() {
            return new FakeNovomaticState(events,
                    currentEngine,
                    balance,
                    lines,
                    stake,
                    gamblerHistory,
                    gamblerStep,
                    gamblerWinMeter);
        }
    }


}
