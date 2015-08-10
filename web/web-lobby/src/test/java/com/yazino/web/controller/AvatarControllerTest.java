package com.yazino.web.controller;

import com.yazino.platform.player.Avatar;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.form.AvatarForm;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvatarControllerTest {

    @Mock
    private MultipartFile multipartFile;
    @Mock
    private AvatarRepository avatarRepository;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletResponse response;

    private AvatarController underTest;

    @Before
    public void init() throws IOException {
        final byte[] fileBytes = new byte[0];
        when(multipartFile.getOriginalFilename()).thenReturn("aFilename");
        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(avatarRepository.storeAvatar("aFilename", fileBytes))
                .thenReturn(new Avatar("aPictureLocation", "aUrl"));

        underTest = new AvatarController(avatarRepository, webApiResponses);
    }

    @Test
    public void listAvatarsShouldReturnValidJson() throws IOException {
        final List<Map<String, Object>> expectedAvatars = new ArrayList<>();
        expectedAvatars.add(jsonAvatar("location1", "url1"));
        expectedAvatars.add(jsonAvatar("location2", "url2"));
        final Map<String, Object> expectedJson = Collections.<String, Object>singletonMap("avatars", expectedAvatars);
        final List<Avatar> avatars = Arrays.asList(new Avatar("location1", "url1"), new Avatar("location2", "url2"));
        when(avatarRepository.retrieveAvailableAvatars()).thenReturn(avatars);

        underTest.showAvailableAvatars(response);

        verify(webApiResponses).writeOk(response, expectedJson);
    }

    @Test
    public void shouldReturnAvatarJsonOnUploadAvatar() throws IOException {
        final Map<String, Object> expectedJson = new HashMap<>();
        expectedJson.put("avatar", jsonAvatar("aPictureLocation", "aUrl"));

        underTest.uploadAvatar(new AvatarForm(multipartFile), response);

        verify(webApiResponses).writeOk(response, expectedJson);
    }

    private Map<String, Object> jsonAvatar(final String pictureLocation,
                                           final String url) {
        final Map<String, Object> json = new HashMap<>();
        json.put("pictureLocation", pictureLocation);
        json.put("url", url);
        return json;
    }
}
