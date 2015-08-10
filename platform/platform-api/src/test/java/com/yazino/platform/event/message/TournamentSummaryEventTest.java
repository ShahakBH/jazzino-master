package com.yazino.platform.event.message;

import com.yazino.platform.JsonHelper;
import com.yazino.platform.messaging.Jackson2JodaJsonMessageConverter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class TournamentSummaryEventTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.ONE;
    private static final String TROURNAMENT_NAME = "aTrournamentName";
    private static final BigDecimal TEMPLATE_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "aGameType";
    private static final DateTime START_TS = new DateTime(DateTimeZone.UTC);
    private static final DateTime FINISHED_TS = new DateTime(DateTimeZone.UTC).plus(10);

    @Test
    public void shouldSerializeToJsonAndDeserializeWithPlayers() {
        final TournamentSummaryEvent event = anEvent();
        final JsonHelper jsonHelper = new JsonHelper();

        String serialized = jsonHelper.serialize(event);
        TournamentSummaryEvent deserializedEvent = jsonHelper.deserialize(TournamentSummaryEvent.class, serialized);
        assertEquals(1, deserializedEvent.getPlayers().size());
    }

    @Test
    public void shouldSerializeToMessageAndDeserializeWithPlayers() {
        final TournamentSummaryEvent event = anEvent();
        final Jackson2JsonMessageConverter messageConverter = new Jackson2JodaJsonMessageConverter();

        final Message message = messageConverter.toMessage(event, new MessageProperties());
        final TournamentSummaryEvent deserialised = (TournamentSummaryEvent) messageConverter.fromMessage(message);
        assertThat(deserialised, is(equalTo(event)));
    }

    private TournamentSummaryEvent anEvent() {
        final List<TournamentPlayerSummary> players = new ArrayList<>();
        players.add(new TournamentPlayerSummary(BigDecimal.ONE, 1, BigDecimal.TEN));
        return new TournamentSummaryEvent(TOURNAMENT_ID,
                TROURNAMENT_NAME,
                TEMPLATE_ID,
                GAME_TYPE,
                START_TS,
                FINISHED_TS,
                players);
    }
}
