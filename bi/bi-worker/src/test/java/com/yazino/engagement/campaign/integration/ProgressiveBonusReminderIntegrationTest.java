package com.yazino.engagement.campaign.integration;

import com.yazino.engagement.ChannelType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProgressiveBonusReminderIntegrationTest {

    private SystemUnderTest sut;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 6, 28, 11, 0).getMillis());
        sut = new SystemUnderTest();
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void sendNotificationsToPlayersWhoPlayedYesterday() {
        sut.createPlayers("player1", "player2");
        sut.playedYesterday("player1");
        sut.createCampaign("progressive_bonus", "this is the message", now(), Boolean.FALSE);
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessage("player1", ChannelType.IOS, "this is the message"), is(true));
        assertThat(sut.playerReceivedMessage("player2", ChannelType.IOS, "this is the message"), is(false));
    }

    @Test
    public void sendNotificationsToMultipleChannels() {
        final SystemUnderTest sut = new SystemUnderTest();
        sut.createPlayers("player1", "player2");
        sut.playedYesterday("player1");
        sut.createCampaign("progressive_bonus", "this is the message", now(), Boolean.FALSE);
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessage("player1", ChannelType.IOS, "this is the message"), is(true));
        assertThat(sut.playerReceivedMessage("player1", ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, "this is the message"), is(true));
        assertThat(sut.playerReceivedMessage("player1", ChannelType.FACEBOOK_APP_TO_USER_REQUEST, "this is the message"), is(true));
    }

    @Test
    public void multipleRunsOfTheSameCampaign() {
        sut.createPlayers("player1", "player2");
        sut.playedYesterday("player1");
        sut.createCampaign("progressive_bonus", "this is the message", now(), Boolean.FALSE);
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessage("player1", ChannelType.IOS, "this is the message"), is(true));
        assertThat(sut.playerReceivedMessage("player2", ChannelType.IOS, "this is the message"), is(false));
        sut.onNextWeek();
        sut.playedYesterday("player2");
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessage("player2", ChannelType.IOS, "this is the message"), is(true));
        assertThat(sut.playerReceivedMessage("player1", ChannelType.IOS, "this is the message"), is(false));
    }

    @Test
    public void sendNotificationUsingDifferentCampaignForEachDayOfTheWeek() {
        sut.createPlayers("player1", "player2", "player3");
        sut.createCampaign("progressive_bonus1", "this is the message day 1", now(), Boolean.FALSE);
        sut.createCampaign("progressive_bonus2", "this is the message day 2", now().plus(aDay()), Boolean.FALSE);
        sut.createCampaign("progressive_bonus3", "this is the message day 3", now().plus(2 * aDay()), Boolean.FALSE);
        sut.playedYesterday("player1");
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessages("player1"), equalTo(messages("this is the message day 1")));
        assertThat(sut.playerReceivedMessages("player2"), equalTo(noMessages()));
        assertThat(sut.playerReceivedMessages("player3"), equalTo(noMessages()));
        sut.onTheNextDay();
        sut.playedYesterday("player3");
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessages("player1"), equalTo(noMessages()));
        assertThat(sut.playerReceivedMessages("player2"), equalTo(noMessages()));
        assertThat(sut.playerReceivedMessages("player3"), equalTo(messages("this is the message day 2")));
        sut.onTheNextDay();
        sut.playedYesterday("player2");
        sut.playedYesterday("player1");
        sut.schedulerRunsCampaign();
        assertThat(sut.playerReceivedMessages("player1"), equalTo(messages("this is the message day 3")));
        assertThat(sut.playerReceivedMessages("player2"), equalTo(messages("this is the message day 3")));
        assertThat(sut.playerReceivedMessages("player3"), equalTo(noMessages()));
    }

    private HashSet<String> noMessages() {
        return new HashSet<String>();
    }

    private HashSet<String> messages(String... messages) {
        return new HashSet<String>(asList(messages));
    }


    private DateTime now() {
        return new DateTime();
    }

    private long aDay() {
        return DateTimeConstants.MILLIS_PER_DAY;
    }
}

