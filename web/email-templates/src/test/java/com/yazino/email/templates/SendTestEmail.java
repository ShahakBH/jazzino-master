package com.yazino.email.templates;

import com.yazino.email.EmailException;

public class SendTestEmail {

    public static final String PROJECT_PATH = System.getProperty("user.home")
            + "/Projects/strata/strata/strata.server/platform/email-templates";

    private static final SendTestEmailHelper SEND_TEST_EMAIL_HELPER = new SendTestEmailHelper(PROJECT_PATH);

    public static void main(String[] args) throws EmailException {
        SEND_TEST_EMAIL_HELPER.usingTemplate("challenge-buddies")
                .toAddresses("jrae@yazino.com")
//                .andAddress("nadiashaik@gmail.com")
//                .andAddress("alyssa@yazino.com")
                .withProperty("gameType", "BLACKJACK")
                .withProperty("userName", "Matt")
                .withProperty("userName", "Matt")
                .withProperty("targetUrl", "https://www.yazino.com/?ref=email_reflex_invite_reminder")
                .withSubject("[test] Matt is waiting for you to play at Yazino")
                .send();
    }
}
