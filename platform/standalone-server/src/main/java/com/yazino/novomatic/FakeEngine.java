package com.yazino.novomatic;

public enum FakeEngine {
    Standard(new FakeEngineRulesForStandardGame()),
    Gambler(new FakeEngineRulesForGambler());

    private final FakeEngineRules engine;

    public FakeEngineRules getEngine() {
        return engine;
    }

    FakeEngine(FakeEngineRules engine) {
        this.engine = engine;
    }
}
