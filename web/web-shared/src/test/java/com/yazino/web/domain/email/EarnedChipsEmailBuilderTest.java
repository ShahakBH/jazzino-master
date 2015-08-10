package com.yazino.web.domain.email;

import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EarnedChipsEmailBuilderTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(999);
    private static final String IDENTIFIER = "Earned Id";
    private static final BigDecimal CHIPS = BigDecimal.valueOf(1100000);
    private static final String EMAIL_ADDRESS = "just@me.com";
    private static final String FIRST_NAME = "playerFirstName";

    private final PlayerProfile profile = new PlayerProfile(EMAIL_ADDRESS, "DisplayName", "RealName", Gender.MALE, "USA", FIRST_NAME, "Last", new DateTime(), "ReferralIdentifier", "Provider", "rpxProvider", "externalid", false);
    private final PlayerProfileService profileService = mock(PlayerProfileService.class);
    private final EarnedChipsEmailBuilder builder = new EarnedChipsEmailBuilder(PLAYER_ID, IDENTIFIER, CHIPS);

    @Before
    public void setup() {
        when(profileService.findByPlayerId(PLAYER_ID)).thenReturn(profile);
    }

    @Test
    public void shouldBuildRequestWithCorrectEmailAddress() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(newHashSet(EMAIL_ADDRESS), request.getAddresses());
    }

    @Test
    public void shouldBuildRequestWithCorrectSubject() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("Thanks for contributing, playerFirstName!", request.getSubject());
    }

    @Test
    public void shouldBuildRequestWithCorrectTemplate() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("ConfirmationOfEarnedChipsEmail.vm", request.getTemplate());
    }

    @Test
    public void shouldBuildRequestWithCorrectProperties() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        Map<String,Object> properties = request.getProperties();
        assertEquals(3, properties.size());
        assertEquals(FIRST_NAME, properties.get("playerFirstName"));
        assertEquals("1,100,000", properties.get("earnedChips"));
        assertEquals(IDENTIFIER, properties.get("earnedId"));
    }
    
}
