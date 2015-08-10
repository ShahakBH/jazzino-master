package com.yazino.novomatic;

import com.yazino.novomatic.cgs.*;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class FakeEngineRulesForStandardGame implements FakeEngineRules {

    private static final List<Long> LINES = asList(1L, 3L, 5L, 7L, 9L);
    private static final List<Long> STAKES = asList(1L, 2L, 3L, 4L, 5L, 10L);
    private static final long WINNER_LINES = 5L, WINNER_STAKE = 5L;
    public static final long INITIAL_LINES = 9L;
    public static final long INITIAL_STAKE = 1L;
    public static final long WIN_AMOUNT = 100L;
    private static final List<Long> DENOMS = asList(1L);
    private static long denom;

    public static FakeNovomaticState initialState(long initialBalance) {
        denom = 1L;
        return new FakeNovomaticState.Builder()
                .setEngine(FakeEngine.Standard)
                .setBalance(initialBalance)
                .setLines(INITIAL_LINES)
                .setStake(INITIAL_STAKE)
                .addEvents(
                        NovomaticEventType.EventGameStart,
                        new GameParameters(new SelectableValue(LINES, INITIAL_LINES),
                                new SelectableValue(STAKES, INITIAL_STAKE),
                                new SelectableValue(DENOMS, denom),
                                creditFor(initialBalance),
                                initialBalance),
                        new ReelsRotate(asList("AAA", "BBB", "CCC", "DDD", "EEE")),
                        NovomaticEventType.EventGameEnd,
                        NovomaticEventType.EventGameStart
                ).build();
    }

    @Override
    public FakeNovomaticState process(FakeNovomaticState gameState, NovomaticCommand command) {
        final FakeNovomaticState.Builder builder = new FakeNovomaticState.Builder(gameState);
        final int currentIndex;
        final long newLines, newStake;
        final long lines = gameState.getLines();
        final long stake = gameState.getStake();
        final long balance = gameState.getBalance();
        switch (command) {
            case Spin:
                long balanceAfterSpin = balance - (stake * lines);
                if (lines == WINNER_LINES && stake == WINNER_STAKE) {
                    long balanceAfterWin = balance + WIN_AMOUNT;
                    builder.setBalance(balanceAfterWin)
                            .setEngine(FakeEngine.Gambler)
                            .setGamblerHistory("")
                            .setGamblerStep(0L)
                            .setGamblerWinMeter(WIN_AMOUNT)
                            .addEvents(new CreditChanged(creditFor(balanceAfterSpin), balanceAfterSpin),
                                    new ReelsRotate(asList("AAA", "BBB", "CCC", "DDD", "EEE")),
                                    new CreditWon(Arrays.asList(1L, 0L, 3L, 0L, 4L, 0L), 18L, "E", 18L, 1L),
                                    NovomaticEventType.EventGameEnd,
                                    NovomaticEventType.EventGamblerStart);
                    return builder.build();
                }
                builder.setBalance(balanceAfterSpin);
                builder.addEvents(
                        new CreditChanged(creditFor(balanceAfterSpin), balanceAfterSpin),
                        new ReelsRotate(asList("AAA", "BBB", "CCC", "DDD", "EEE")),
                        NovomaticEventType.EventGameEnd,
                        NovomaticEventType.EventGameStart
                );
                return builder.build();
            case DecreaseLines:
                currentIndex = LINES.indexOf(lines);
                if (currentIndex < 1) {
                    return gameState;
                }
                newLines = LINES.get(currentIndex - 1);
                builder.setLines(newLines);
                builder.addEvents(new GameParameters(new SelectableValue(LINES, newLines), new SelectableValue(STAKES, stake),
                        new SelectableValue(DENOMS, denom), creditFor(balance), balance));
                return builder.build();
            case IncreaseLines:
                currentIndex = LINES.indexOf(lines);
                if (currentIndex < 0 || currentIndex == LINES.size() - 1) {
                    return gameState;
                }
                newLines = LINES.get(currentIndex + 1);
                builder.setLines(newLines);
                builder.addEvents(new GameParameters(new SelectableValue(LINES, newLines), new SelectableValue(STAKES, stake),
                        new SelectableValue(DENOMS, denom), creditFor(balance), balance));
                return builder.build();
            case IncreaseBetPlacement:
                currentIndex = STAKES.indexOf(stake);
                if (currentIndex < 0 || currentIndex == STAKES.size() - 1) {
                    return gameState;
                }
                newStake = STAKES.get(currentIndex + 1);
                builder.setStake(newStake);
                builder.addEvents(new GameParameters(new SelectableValue(LINES, lines), new SelectableValue(STAKES, newStake),
                        new SelectableValue(DENOMS, denom), creditFor(balance), balance));
                return builder.build();
            case DecreaseBetPlacement:
                currentIndex = STAKES.indexOf(stake);
                if (currentIndex < 1) {
                    return gameState;
                }
                newStake = STAKES.get(currentIndex - 1);
                builder.setStake(newStake);
                builder.addEvents(new GameParameters(new SelectableValue(LINES, lines), new SelectableValue(STAKES, newStake),
                        new SelectableValue(DENOMS, denom), creditFor(balance), balance));
                return builder.build();
            default:
                return gameState;
        }
    }

    private static long creditFor(long balance) {
        return balance / 10;
    }

}
