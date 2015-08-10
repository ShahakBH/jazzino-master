package com.yazino.platform.model.tournament;

import com.yazino.platform.repository.table.GameTypeRepository;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameMetaDataKey;

import static org.apache.commons.lang3.Validate.notNull;

public enum TournamentAchievement {
    COMPETITION_1_OUT_OF_10(1, 10, 19),
    COMPETITION_1_OUT_OF_20(1, 20, 49),
    COMPETITION_1_OUT_OF_50(1, 50, 99),
    COMPETITION_1_OUT_OF_100(1, 100, 199),
    COMPETITION_1_OUT_OF_200(1, 200, 499),
    COMPETITION_1_OUT_OF_500(1, 500, 999),
    COMPETITION_1_OUT_OF_1000(1, 1000, 9999),
    COMPETITION_2_OUT_OF_10(2, 10, 19),
    COMPETITION_2_OUT_OF_20(2, 20, 49),
    COMPETITION_2_OUT_OF_50(2, 50, 99),
    COMPETITION_2_OUT_OF_100(2, 100, 199),
    COMPETITION_2_OUT_OF_200(2, 200, 499),
    COMPETITION_2_OUT_OF_500(2, 500, 999),
    COMPETITION_2_OUT_OF_1000(2, 1000, 9999),
    COMPETITION_3_OUT_OF_10(3, 10, 19),
    COMPETITION_3_OUT_OF_20(3, 20, 49),
    COMPETITION_3_OUT_OF_50(3, 50, 99),
    COMPETITION_3_OUT_OF_100(3, 100, 199),
    COMPETITION_3_OUT_OF_200(3, 200, 499),
    COMPETITION_3_OUT_OF_500(3, 500, 999),
    COMPETITION_3_OUT_OF_1000(3, 1000, 9999);

    private final int position;
    private final int minimumPlayers;
    private final int maximumPlayers;

    private TournamentAchievement(final int position,
                                  final int minimumPlayers,
                                  final int maximumPlayers) {
        this.position = position;
        this.minimumPlayers = minimumPlayers;
        this.maximumPlayers = maximumPlayers;
    }

    public static String getAchievement(final GameTypeRepository gameTypeRepository,
                                        final String gameTypeId,
                                        final int position,
                                        final int numberOfPlayers) {
        notNull(gameTypeId, "Game type is null");

        for (final TournamentAchievement achievement : values()) {
            if (position == achievement.position
                    && numberOfPlayers >= achievement.minimumPlayers
                    && numberOfPlayers <= achievement.maximumPlayers) {
                return String.format("%s_%s",
                        achievementPrefixFor(gameTypeRepository, gameTypeId),
                        achievement.name());
            }
        }
        return null;
    }

    private static String achievementPrefixFor(final GameTypeRepository gameTypeRepository,
                                               final String gameTypeId) {
        final GameMetaData gameMetaData = gameTypeRepository.getMetaDataFor(gameTypeId);
        if (gameMetaData != null) {
            final String achievementPrefix = gameMetaData.forKey(GameMetaDataKey.TOURNAMENT_ACHIEVEMENT_PREFIX);
            if (achievementPrefix != null) {
                return achievementPrefix;
            }
        }
        return gameTypeId;
    }
}
