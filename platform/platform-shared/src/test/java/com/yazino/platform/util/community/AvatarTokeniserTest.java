package com.yazino.platform.util.community;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AvatarTokeniserTest {
    private static final String CONTENT_URL = "https://a.host/content-100000";
    private static final String AVATAR_URL = "https://a.host/avatars";

    private static final String AVATAR_FACEBOOK = "http://graph.facebook.com/1389746532/picture";

    private AvatarTokeniser underTest;

    @Before
    public void setUp() {
        final List<String> avatarPatterns = Arrays.asList(
                "^https?://(www\\.)?a.host/avatars",
                "^https?://(www\\.)?an.old.host/avatars",
                "^https?://cdn(-small)?.yazino.com/web-content-\\d+T\\d+/avatars");
        final List<String> contentPatterns = Arrays.asList(
                "^https?://cdn(-small)?.yazino.com/(public/)?(web-)?content-\\d+T\\d+",
                "^https?://content.yazino.com/content/\\d+");

        underTest = new AvatarTokeniser(CONTENT_URL, contentPatterns, AVATAR_URL, avatarPatterns);
    }

    @Test(expected = NullPointerException.class)
    public void aTokeniserCannotBeCreatedWithANullContentUrl() {
        new AvatarTokeniser(null, Collections.<String>emptyList(), AVATAR_URL, Collections.<String>emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void aTokeniserCannotBeCreatedWithANullAvatarUrl() {
        new AvatarTokeniser(CONTENT_URL, Collections.<String>emptyList(), null, Collections.<String>emptyList());
    }

    @Test
    public void tokenisingANullAvatarReturnsNull() {
        assertThat(underTest.tokenise(null), is(nullValue()));
    }

    @Test
    public void tokenisingAYazinoAvatarTokenisesTheAvatarUrl() {
        final String tokenise = underTest.tokenise("http://a.host/avatars/public/avatar1.png");

        assertThat(tokenise, is(equalTo("%AVATARS%/public/avatar1.png")));
    }

    @Test
    public void tokenisingAnOldYazinoAvatarTokenisesTheAvatarUrl() {
        final String tokenise = underTest.tokenise("https://an.old.host/avatars/public/avatar1.png");

        assertThat(tokenise, is(equalTo("%AVATARS%/public/avatar1.png")));
    }

    @Test
    public void tokenisingAContentBasedAvatarTokenisesTheContentUrl() {
        final String tokenise = underTest.tokenise(
                "https://cdn.yazino.com/web-content-20100502T1234566/images/avatar1.png");

        assertThat(tokenise, is(equalTo("%CONTENT%/images/avatar1.png")));
    }

    @Test
    public void tokenisingALegacyContentBasedAvatarWhereTheUrlHasASingleContentDirectoryTokenisesTheAvatarUrl() {
        final String tokenise = underTest.tokenise("http://cdn.yazino.com/content-20111122T181640/images/avatar1.png");

        assertThat(tokenise, is(equalTo("%CONTENT%/images/avatar1.png")));
    }

    @Test
    public void tokenisingALegacyContentBasedAvatarWhereTheUrlIsASubDirOfPublicContentTokenisesTheAvatarUrl() {
        final String tokenise = underTest.tokenise("http://cdn.yazino.com/public/web-content-20120125T064146/images/avatar1.png");

        assertThat(tokenise, is(equalTo("%CONTENT%/images/avatar1.png")));
    }

    @Test
    public void tokenisingAnAvatarWhereTheUrlIsASubDirOfContentTokenisesTheAvatarUrl() {
        final String tokenise = underTest.tokenise(
                "https://cdn.yazino.com/web-content-20100502T1234566/avatars/images/avatar1.png");

        assertThat(tokenise, is(equalTo("%AVATARS%/images/avatar1.png")));
    }

    @Test
    public void tokenisingAFacebookAvatarDoesNotChangeIt() {
        assertThat(underTest.tokenise(AVATAR_FACEBOOK), is(equalTo(AVATAR_FACEBOOK)));
    }

    @Test
    public void detokenisingANullTokenisedAvatarReturnsNull() {
        assertThat(underTest.detokenise(null), is(nullValue()));
    }

    @Test
    public void detokenisingAFacebookTokenisedAvatarDoesNotChangeIt() {
        assertThat(underTest.detokenise(AVATAR_FACEBOOK), is(equalTo(AVATAR_FACEBOOK)));
    }

    @Test
    public void detokenisingAYazinoTokenisedAvatarInsertsTheAvatarUrl() {
        final String detokenised = underTest.detokenise("%AVATARS%/public/avatar1.png");

        assertThat(detokenised, is(equalTo(AVATAR_URL + "/public/avatar1.png")));
    }

    @Test
    public void detokenisingAContentBasedTokenisedAvatarInsertsTheContentUrl() {
        final String detokenised = underTest.detokenise(
                "%CONTENT%/images/avatar1.png");

        assertThat(detokenised, is(equalTo(CONTENT_URL + "/images/avatar1.png")));
    }

}
