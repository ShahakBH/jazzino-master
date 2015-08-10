package com.yazino.platform.processor.statistic.achievement;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.processor.statistic.PlayerStatisticEventConsumer;
import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Component("achievementStatisticEventsConsumer")
@Qualifier("playerStatisticEventConsumers")
public class AchievementStatisticEventsConsumer implements PlayerStatisticEventConsumer {
    private final AchievementRepository achievementRepository;
    private final Map<String, Accumulator> accumulators = new HashMap<String, Accumulator>();
    private final PlayerAchievementsRepository playerAchievementsRepository;

    @Autowired(required = true)
    public AchievementStatisticEventsConsumer(
            @Qualifier("achievementAccumulators") final Collection<Accumulator> accumulators,
            @Qualifier("achievementRepository") final AchievementRepository achievementRepository,
            final PlayerAchievementsRepository playerAchievementsRepository) {
        notNull(accumulators, "accumulators is null");
        notNull(achievementRepository, "achievementRepository is null");
        notNull(playerAchievementsRepository, "playerAchievementsRepository is null");
        this.achievementRepository = achievementRepository;
        this.playerAchievementsRepository = playerAchievementsRepository;
        updateAccumulators(accumulators);
    }

    private void updateAccumulators(final Collection<Accumulator> newAccumulators) {
        for (final Accumulator accumulator : newAccumulators) {
            this.accumulators.put(accumulator.getName(), accumulator);
        }
    }

    @Override
    public void processEvents(final BigDecimal playerId,
                              final String gameType,
                              final Collection<StatisticEvent> events) {
        final Collection<Achievement> achievements = achievementRepository.findByGameType(gameType);
        final PlayerAchievements playerAchievements = playerAchievementsRepository.forPlayer(playerId);
        final boolean hasChanges = processAllAchievements(events, achievements, playerAchievements);
        if (hasChanges) {
            playerAchievementsRepository.save(playerAchievements);
        }
    }

    private boolean processAllAchievements(final Collection<StatisticEvent> events,
                                           final Collection<Achievement> achievements,
                                           final PlayerAchievements playerAchievements) {
        boolean hasChanges = false;
        for (final Achievement achievement : achievements) {
            if (!achievement.isRecurring() && playerAchievements.hasAchievement(achievement.getId())) {
                continue;
            }
            final StatisticEvent[] achievementEvents = eventsByNames(events, achievement.getEvents());
            boolean eventsProcessed;
            if (achievementEvents.length > 0) {
                eventsProcessed = processAchievement(playerAchievements, achievement, achievementEvents);
            } else {
                eventsProcessed = processAchievement(playerAchievements, achievement, (StatisticEvent[]) null);
            }
            hasChanges = hasChanges || eventsProcessed;
        }
        return hasChanges;
    }

    private boolean processAchievement(final PlayerAchievements player,
                                       final Achievement achievement,
                                       final StatisticEvent... events) {
        if (events == null || achievement.accepts(events)) {
            final Accumulator accumulator = accumulators.get(achievement.getAccumulator());
            if (accumulator == null) {
                throw new IllegalStateException("Accumulator not configured: " + achievement.getAccumulator());
            }
            return accumulator.accumulate(player, achievement, events);
        }
        return false;
    }

    private StatisticEvent[] eventsByNames(final Collection<StatisticEvent> events,
                                           final Collection<String> eventNames) {
        notNull(eventNames, "Event Name may not be null");
        if (events == null || eventNames.size() == 0) {
            return new StatisticEvent[0];
        }
        final Iterable<StatisticEvent> matchingEvents = Iterables.filter(events,
                new Predicate<StatisticEvent>() {
                    @Override
                    public boolean apply(final StatisticEvent achievementEvent) {
                        return eventNames.contains(achievementEvent.getEvent());
                    }
                });
        return Iterables.toArray(matchingEvents, StatisticEvent.class);
    }
}
