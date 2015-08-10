package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentAchievement;
import com.yazino.platform.repository.table.GameTypeRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameMetaDataBuilder;
import com.yazino.game.api.GameMetaDataKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class TournamentAchievementTest {

    private static final String GAME_TYPE_WITH_PREFIX = "aGameWithAPrefix";
    private static final String GAME_TYPE_WITHOUT_PREFIX = "aGameWithoutAPrefix";
    private static final String ACHIEVEMENT_PREFIX = "aGamePrefix";
    @Mock
    private GameTypeRepository gameTypeRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(gameTypeRepository.getMetaDataFor(GAME_TYPE_WITH_PREFIX)).thenReturn(
                new GameMetaDataBuilder()
                        .with(GameMetaDataKey.TOURNAMENT_ACHIEVEMENT_PREFIX, ACHIEVEMENT_PREFIX)
                        .build());
    }

    @Test
    public void whereATournamentPrefixExistsItIsUsedAsThePrefixForTheAchievement() {
        final String achievement = TournamentAchievement.getAchievement(
                gameTypeRepository, GAME_TYPE_WITH_PREFIX, 2, 100);

        assertThat(achievement, is(equalTo(ACHIEVEMENT_PREFIX + "_COMPETITION_2_OUT_OF_100")));
    }

    @Test
    public void whereNoTournamentPrefixExistsTheGameTypeIDIsUsedAsThePrefixForTheAchievement() {
        final String achievement = TournamentAchievement.getAchievement(
                gameTypeRepository, GAME_TYPE_WITHOUT_PREFIX, 1, 1002);

        assertThat(achievement, is(equalTo(GAME_TYPE_WITHOUT_PREFIX + "_COMPETITION_1_OUT_OF_1000")));
    }

}
