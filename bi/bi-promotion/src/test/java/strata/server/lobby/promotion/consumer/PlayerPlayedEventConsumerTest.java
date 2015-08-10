package strata.server.lobby.promotion.consumer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;
import com.yazino.platform.event.message.PlayerPlayedEvent;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerPlayedEventConsumerTest {

    @Mock
    PlayerPromotionStatusDao playerPromotionStatusDao;

    private PlayerPlayedEventConsumer underTest;
    public static final DateTime LAST_PLAYED = new DateTime(1977, 2, 12, 5, 30, 0, 0);
    public static final BigDecimal PLAYER_ID = new BigDecimal(1152347);

    @Before
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        underTest = new PlayerPlayedEventConsumer(playerPromotionStatusDao);
    }

    @Test
    public void handleShouldUpdateLastPlayedDateIfDateNullAndConsecutiveDaysToOne() throws Exception {
        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayedDateAsTimestamp(null)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);
        DateTime lastTimePlayerPlayed = new DateTime();
        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));
        verify(playerPromotionStatusDao).get(PLAYER_ID);
        final PlayerPromotionStatus expectedPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(1)
                .build();
        verify(playerPromotionStatusDao).save(expectedPromotionStatus);
    }


    @Test
    public void handleShouldUpdateDaysPlayedIfNotDateNullAndInConsecutiveBonusPeriod() throws Exception {
        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(LAST_PLAYED)
                .withConsecutiveDaysPlayed(1)
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);
        DateTime lastTimePlayerPlayed = LAST_PLAYED.plusDays(1);
        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));

        final PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(2)
                .build();

        verify(playerPromotionStatusDao).save(expectedPlayerPromotionStatus);
    }

    @Test
    public void handleShouldSetConsecutiveDaysPlayedToOneIfOverTwentyFourHoursSinceLastPlay() throws Exception {
        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(LAST_PLAYED)
                .withConsecutiveDaysPlayed(4)
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);
        DateTime lastTimePlayerPlayed = LAST_PLAYED.plusDays(2);
        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));

        final PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(1)
                .build();

        verify(playerPromotionStatusDao).save(expectedPlayerPromotionStatus);
    }

    @Test
    public void handleShouldUpdateDaysPlayedIfDatePlayedInNextPromotionTimeWindow() throws Exception {

        DateTimeZone timeZone = DateTimeZone.forID("Europe/London");
        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(new DateTime(1977, 2, 12, 4, 59, 0, 0, timeZone))
                .withConsecutiveDaysPlayed(1)
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);

        DateTime lastTimePlayerPlayed = new DateTime(1977, 2, 12, 5, 1, 0, 0, timeZone);

        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));

        final PlayerPromotionStatus exPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(2)
                .build();

        verify(playerPromotionStatusDao).save(exPlayerPromotionStatus);
    }

    @Test
    public void handleShouldNotUpdateDaysPlayedIfDatePlayedIfInSamePromotionTimeWindow() throws Exception {

        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(new DateTime(1977, 2, 12, 4, 0, 0, 0))
                .withConsecutiveDaysPlayed(1)
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);

        DateTime lastTimePlayerPlayed = new DateTime(1977, 2, 12, 4, 30, 0, 0);

        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));

        final PlayerPromotionStatus exPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(1)
                .build();

        verify(playerPromotionStatusDao).save(exPlayerPromotionStatus);
    }

    @Test
    public void handleShouldNotAddMoreThanFiveConsecutiveDays() throws Exception {

        PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(new DateTime(1977, 2, 12, 4, 0, 0, 0))
                .withConsecutiveDaysPlayed(5)
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);

        DateTime lastTimePlayerPlayed = new DateTime(1977, 2, 13, 4, 30, 0, 0);

        underTest.handle(new PlayerPlayedEvent(PLAYER_ID, lastTimePlayerPlayed));

        final PlayerPromotionStatus exPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(lastTimePlayerPlayed)
                .withConsecutiveDaysPlayed(5)
                .build();

        verify(playerPromotionStatusDao).save(exPlayerPromotionStatus);
    }

}