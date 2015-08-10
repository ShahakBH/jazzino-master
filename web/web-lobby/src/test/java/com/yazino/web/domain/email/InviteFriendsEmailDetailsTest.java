package com.yazino.web.domain.email;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class InviteFriendsEmailDetailsTest {

    private static final BigDecimal PLAYER_ID = new BigDecimal(-1592);

    @Test
    public void shouldBuildCallToActionUrl() {
        InviteFriendsEmailDetails underTest = new InviteFriendsEmailDetails("a msg", "http://yazino.com/ref=%s", PLAYER_ID);
        assertThat(underTest.getCallToActionUrl(), equalTo("http://yazino.com/ref=-1592"));
    }


}


