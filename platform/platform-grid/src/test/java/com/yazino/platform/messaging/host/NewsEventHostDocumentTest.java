package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.destination.Destination;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NewsEventHostDocumentTest {
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private Destination destination;

    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ONE = BigDecimal.ONE;

    @Test
    public void shouldBuildNewsEventWithPostedAchievementDetails() throws Exception {
        String postedTitle = "Yee ha - you're awesome!";
        final String postedLink = "Posted link name";
        final String actionText = "Action text ";
        final String actionLink = "Action link ";
        final NewsEvent achievementEvent = new NewsEvent.Builder(PLAYER_ONE, new ParameterisedMessage("Achievement"))
                .setPostedAchievementTitleText(postedTitle)
                .setPostedAchievementTitleLink(postedLink)
                .setPostedAchievementActionText(actionText)
                .setPostedAchievementActionLink(actionLink)
                .setTitle("Wild Child")
                .setShortDescription(new ParameterisedMessage("Player one won."))
                .setGameType("HIGH_STAKES")
                .build();

        new NewsEventHostDocument(TABLE_ID, "aPartnerId", achievementEvent, destination).send(documentDispatcher);

        final ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        final Document document = documentCaptor.getValue();
        assertThat(document.getBody(), containsString(escaped(postedTitle)));
        assertThat(document.getBody(), containsString(escaped(postedLink)));
        assertThat(document.getBody(), containsString(escaped(actionText)));
        assertThat(document.getBody(), containsString(escaped(actionLink)));
    }

    private String escaped(String json) {
        return json.replaceAll("\"", "\\\\\"");
    }
}
