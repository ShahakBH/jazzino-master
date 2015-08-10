package com.yazino.web.domain;

import com.yazino.platform.player.Avatar;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AvatarRepositoryTest {
    private static final String URL = "http://avatar.server";
    private static final String TEST_AVATARS_PATH = "/avatars";

    private AvatarRepository underTest;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        underTest = new AvatarRepository();
        underTest.setPath(URLDecoder.decode(this.getClass().getResource(TEST_AVATARS_PATH).getPath(), "UTF-8"));
        underTest.setUrl(URL);
    }

	@Test
	public void retrievingTheDefaultAvatarReturnsTheFirstAvatarInTheList() {
		final Avatar avatar = underTest.retrieveDefaultAvatar();

        final String pictureLocation = AvatarRepository.PUBLIC_SUFFIX + "/avatar0.png";
        final String pictureURL = URL + "/" + pictureLocation;
		assertThat(avatar, is(equalTo(new Avatar(pictureLocation, pictureURL))));
	}

	@Test
	public void retrievingTheDefaultAvatarFromAnInvalidPathReturnsADummyAvatar() {
        underTest.setPath("/an/invalid/path");

		final Avatar avatar = underTest.retrieveDefaultAvatar();

		assertThat(avatar, is(equalTo(new Avatar("#", "#"))));
	}

	@Test
	public void retrieveAvatarsFromAnInvalidPathReturnsAnEmptyList() {
        underTest.setPath("/an/invalid/path");

		final List<Avatar> listOfAvatars = underTest.retrieveAvailableAvatars();

		assertThat(listOfAvatars, is(equalTo(Collections.<Avatar>emptyList())));
	}

    @Test
    public void testRetrieveAvatars() {
        final List<Avatar> actual = underTest.retrieveAvailableAvatars();
        final List<Avatar> expected = new ArrayList<Avatar>();
        for (int i = 0; i < 5; i++) {
            final String pictureLocation = AvatarRepository.PUBLIC_SUFFIX + "/avatar" + i + ".png";
            final String pictureURL = URL + "/" + pictureLocation;
            expected.add(new Avatar(pictureLocation, pictureURL));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testStoreAvatar() throws UnsupportedEncodingException {
        final Avatar newAvatar = underTest.storeAvatar("myImage.gif", new byte[0]);
        final String pictureLocation = newAvatar.getPictureLocation();
        final File createdFile = new File(URLDecoder.decode(this.getClass().getResource(TEST_AVATARS_PATH + "/" + pictureLocation).getPath(), "UTF-8"));
        assertTrue(createdFile.exists());
        assertEquals(URL + "/" + pictureLocation, newAvatar.getUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRefuseStoringAvatarWrongExtension() {
        underTest.storeAvatar("myWrongFile", new byte[0]);
    }
}
