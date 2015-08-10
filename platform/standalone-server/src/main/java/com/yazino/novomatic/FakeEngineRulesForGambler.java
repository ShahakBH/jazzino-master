package com.yazino.novomatic;

import com.yazino.novomatic.cgs.GamblerEnd;
import com.yazino.novomatic.cgs.GamblerInfo;
import com.yazino.novomatic.cgs.NovomaticCommand;

import static com.yazino.novomatic.cgs.NovomaticEventType.EventGameStart;

public class FakeEngineRulesForGambler implements FakeEngineRules {


    private static final int GAMBLER_MAX_STEPS = 3;

    @Override
    public FakeNovomaticState process(FakeNovomaticState gameState, NovomaticCommand command) {
        final FakeNovomaticState.Builder builder = new FakeNovomaticState.Builder(gameState);
        final String gamblerHistory = gameState.getGamblerHistory();
        final long gamblerStep = gameState.getGamblerStep();
        final long gamblerWinMeter = gameState.getGamblerWinMeter();
        final long balance = gameState.getBalance();
        switch (command) {
            case GambleOnBlack:
                builder.setGamblerWinMeter(0L)
                        .setBalance(balance)
                        .addEvents(new GamblerInfo(gamblerHistory + "C", "C", 0, 1),
                                   new GamblerEnd(0L, balance),
                                   EventGameStart);
                return builder.build();
            case GambleOnRed:
                final String newGamblerHistory = gamblerHistory + "H";
                final long newGamblerWinMeter = gamblerWinMeter + 1;
                final long newBalance = balance + 1;
                final long newGamblerStep = gamblerStep + 1;
                builder.setGamblerHistory(newGamblerHistory)
                        .setGamblerStep(newGamblerStep)
                        .setGamblerWinMeter(newGamblerWinMeter)
                        .setBalance(newBalance)
                        .addEvents(new GamblerInfo(newGamblerHistory, "H", newGamblerWinMeter, newGamblerStep));
                if (gamblerStep == GAMBLER_MAX_STEPS) {
                    builder.setEngine(FakeEngine.Standard)
                            .setGamblerStep(0l)
                            .setGamblerHistory("")
                            .setGamblerWinMeter(0l)
                            .addEvents(new GamblerEnd(gamblerWinMeter, balance),
                                    EventGameStart);
                }
                return builder.build();
            case CollectGamble:
                builder.setEngine(FakeEngine.Standard)
                        .setGamblerStep(0l)
                        .setGamblerHistory("")
                        .setGamblerWinMeter(0)
                        .addEvents(new GamblerEnd(gamblerWinMeter, balance),
                                   EventGameStart);
                return builder.build();
            default:
                return gameState;
        }
    }
}
