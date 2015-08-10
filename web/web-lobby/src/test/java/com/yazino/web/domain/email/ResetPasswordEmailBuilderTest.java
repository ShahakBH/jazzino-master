package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ResetPasswordEmailBuilderTest {

    private static final String EMAIL_ADDRESS = "Boris_is_a_tw@london.com";
    private static final String NAME = "Bob";
    private static final String PASSWORD = "opensesame";

    private final PlayerProfileService profileService = mock(PlayerProfileService.class);
    private final ResetPasswordEmailBuilder builder = new ResetPasswordEmailBuilder(EMAIL_ADDRESS, NAME, PASSWORD);

    @Test
    public void shouldBuildEmailWithCorrectEmailAddress() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(newHashSet(EMAIL_ADDRESS), request.getAddresses());
    }

    @Test
    public void shouldBuildEmailWithCorrectSubject() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("Hi Bob, your Yazino password update", request.getSubject());
    }

    @Test
    public void shouldBuildEmailWithCorrectTemplate() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("reset-password.vm", request.getTemplate());
    }

    @Test
    public void shouldBuildEmailWithCorrectProperties() throws Exception {
        Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("realName", NAME);
        templateProperties.put("password", PASSWORD);
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(templateProperties, request.getProperties());
    }




}
