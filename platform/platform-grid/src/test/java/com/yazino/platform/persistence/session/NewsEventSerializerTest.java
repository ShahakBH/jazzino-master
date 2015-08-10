package com.yazino.platform.persistence.session;

import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class NewsEventSerializerTest {
    private NewsEventSerializer serializer;

    @Before
    public void setUp() throws Exception {
        serializer = new NewsEventSerializer();
    }

    @Test
    public void shouldSerializeNullNewsEvent() {
        assertEquals("", serializer.serialize(null));
    }

    @Test
    public void shouldSerializeFullNewsEvent() {
        final NewsEvent news = new NewsEvent.Builder(BigDecimal.TEN, new ParameterisedMessage("aMessage for %s", "Player")).setType(NewsEventType.ACHIEVEMENT).setShortDescription(new ParameterisedMessage("shortMessage %s, %s, %s", "1", "2", "3")).setImage("image").setDelay(325).setGameType("aGameType").build();
        final NewsEventSerializer serializer = new NewsEventSerializer();
        final String serialised = serializer.serialize(news);
        NewsEvent deserializedNews = serializer.deserialize(serialised);
        assertEquals(news, deserializedNews);
    }

    @Test
    public void deserialisationOfLegacyFormatShouldWork() {
        final NewsEvent expectedNews = new NewsEvent.Builder(BigDecimal.TEN, new ParameterisedMessage("aMessage for %s", "Player")).setType(NewsEventType.ACHIEVEMENT).setShortDescription(new ParameterisedMessage("shortMessage %s, %s, %s", "1", "2", "3")).setImage("image").setDelay(325).setGameType("aGameType").build();

        final String legacySerialisedNews = "10\n" +
                "ACHIEVEMENT\n" +
                "aMessage for %s\tPlayer\n" +
                "shortMessage %s, %s, %s\t1\t2\t3\n" +
                "image\n" +
                "325\n" +
                "aGameType";
        final NewsEvent deserialised = serializer.deserialize(legacySerialisedNews);

        assertThat(deserialised, is(equalTo(expectedNews)));
    }

    @Test
    public void serialisationShouldHandleEventsWithNonStringParameters() {
        final NewsEvent news = new NewsEvent.Builder(BigDecimal.TEN, new ParameterisedMessage("aMessage for %s on %2$td/%2$tm/%2$tY", "Player", aDate())).setType(NewsEventType.ACHIEVEMENT).setShortDescription(new ParameterisedMessage("shortMessage for %s on %2$td/%2$tm/%2$tY", "Player", aDate())).setImage("image").setDelay(325).setGameType("aGameType").build();

        final String serialised = serializer.serialize(news);
        final NewsEvent deserialised = serializer.deserialize(serialised);

        assertThat(deserialised.getNews().toString(), is(equalTo("aMessage for Player on 03/02/2010")));
        assertThat(deserialised.getShortDescription().toString(), is(equalTo("shortMessage for Player on 03/02/2010")));
    }

    @Test
    public void shouldSerializeIncompleteNewsEvent() {
        final NewsEvent news = new NewsEvent.Builder(BigDecimal.TEN, new ParameterisedMessage("aMessage for %s", "Player")).setImage("image").build();
        final String actual = serializer.serialize(news);
        NewsEvent deserializedNews = serializer.deserialize(actual);
        assertEquals(news, deserializedNews);
    }

    private Date aDate() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2010, Calendar.FEBRUARY, 3, 13, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
