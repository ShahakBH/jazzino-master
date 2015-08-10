package com.yazino.web.form;

import com.yazino.platform.player.PlayerProfile;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;


public class MobileRegistrationFormTest {

    @Test
    public void testToRegistrationRequest() {
        MobileRegistrationForm form = new MobileRegistrationForm();
        form.setEmail("jp@gmail.com");
        form.setPassword("123");
        form.setDisplayName("jack");
        form.setTermsAndConditions(true);
        form.setAvatarURL("locn");

        PlayerProfile profile = form.getUserProfile();

        assertThat(form.getEmail(), is(equalTo(profile.getEmailAddress())));
        assertThat(form.getPassword(), is(equalTo("123")));
        assertThat(form.getDisplayName(), is(equalTo(profile.getDisplayName())));

        assertThat(form.getTermsAndConditions(), is(true));
        assertThat(form.getEmail(), is(equalTo("jp@gmail.com")));
        assertThat(form.getPassword(), is(equalTo("123")));
        assertThat(form.getDisplayName(), is(equalTo("jack")));
        assertThat(form.getAvatarURL(), is(equalTo("locn")));
    }
}
