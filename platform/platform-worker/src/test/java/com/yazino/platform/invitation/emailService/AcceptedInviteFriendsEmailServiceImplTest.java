package com.yazino.platform.invitation.emailService;

import com.yazino.email.EmailException;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AcceptedInviteFriendsEmailServiceImplTest {
    private static final BigDecimal PLAYER_ID = new BigDecimal(-1592);
    private static final BigDecimal USER_PROFILE_ID = new BigDecimal(-1633);
    public static final String FIRST_EMAIL_ADDRESS = "example1@example.com";
    public static final String DISPLAY_NAME = "Pickachu";
    public static final String INVITEE_FIRST_NAME = "Edgar";

    private String SENDER ="from@your.mum";

    private final PlayerProfile userProfile = new PlayerProfile(PLAYER_ID, "test@test.com", DISPLAY_NAME,
            "Test Name", Gender.MALE, "UK", "Mugato", "lastName", new DateTime(1970, 3, 3, 0, 0, 0, 0), null,
            "YAZINO", "rpxProvider", null, true);
    final String[] toAddressArray = new String[2];

    AcceptedInviteFriendsEmailService underTest;
    @Mock
    private AsyncEmailService emailService;
    @Mock
    private InvitationService invitationService;
    @Mock
    private PlayerProfileService playerProfileService;

    @Before
    public void setup() {
        toAddressArray[0] = FIRST_EMAIL_ADDRESS;
        underTest = new AcceptedInviteFriendsEmailServiceImpl(emailService);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(userProfile);
    }

    @Test
    public void testSendInviteFriendsAcceptedEmailSendsMail() throws EmailException {
        final Map<String, Object> map = buildAcceptedInvitePropertyMap();

        assertNotNull(underTest.sendInviteFriendsAcceptedEmail(DISPLAY_NAME, FIRST_EMAIL_ADDRESS, INVITEE_FIRST_NAME, SENDER));
        verify(emailService).send(FIRST_EMAIL_ADDRESS,SENDER,
                AcceptedInviteFriendsEmailServiceImpl.ACCEPTED_INVITE_SUBJECT_TEMPLATE,
                AcceptedInviteFriendsEmailServiceImpl.ACCEPTED_INVITE_EMAIL_TEMPLATE, map);
    }

    @Test
    public void sendInviteFriendsAcceptedEmail_shouldAddInviteeFirstNameToModel() throws EmailException {
        underTest.sendInviteFriendsAcceptedEmail(DISPLAY_NAME, FIRST_EMAIL_ADDRESS, INVITEE_FIRST_NAME, SENDER);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

        verify(emailService).send(anyString(), anyString(),anyString(), anyString(), captor.capture());
        Map templateProperties = captor.getValue();
        assertThat((String) templateProperties.get("inviteeFirstName"), equalTo(INVITEE_FIRST_NAME));
    }

    private Map<String, Object> buildAcceptedInvitePropertyMap() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", DISPLAY_NAME);
        map.put("inviteeFirstName", INVITEE_FIRST_NAME);
        return map;
    }


}

