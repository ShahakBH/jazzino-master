package strata.server.worker.event.consumer.crm;

import com.yazino.engagement.email.application.EmailApi;
import com.yazino.engagement.email.domain.EmailData;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.player.PlayerProfileStatus;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.engagement.email.domain.EmailVisionRestParams.DISPLAY_NAME;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CRMRegistrarTest {

    @Mock
    private EmailApi emailApi;

    @InjectMocks
    private CRMRegistrar underTest = new CRMRegistrar();

    @Test
    public void registerShouldSendEmail() {

        PlayerProfileEvent playerProfileEvent = buildValidUserProfile();
        Map<String, String> dynamicKeys = new LinkedHashMap<String, String>();
        dynamicKeys.put(DISPLAY_NAME.toString(), playerProfileEvent.getDisplayName());

        EmailData emailData = new EmailData(playerProfileEvent.getEmail(), playerProfileEvent.getRegistrationTime(),
                dynamicKeys);
        when(emailApi.sendDayZeroEmail(emailData)).thenReturn(true);

        underTest.register(playerProfileEvent);
    }

    @Test
    public void registerShouldNotSendEmailWhenAddressIsNull() {
        PlayerProfileEvent playerProfileEvent = buildValidUserProfile();
        playerProfileEvent.setEmail(null);

        underTest.register(playerProfileEvent);

        verifyZeroInteractions(emailApi);
    }

    @Test
    public void registerShouldNotBubbleUpExceptionIfEmailSenderThrowsOne() {

        PlayerProfileEvent playerProfileEvent = buildValidUserProfile();
        Map<String, String> dynamicKeys = new LinkedHashMap<String, String>();
        dynamicKeys.put("DISPLAY_NAME", playerProfileEvent.getDisplayName());

        EmailData emailData = new EmailData(playerProfileEvent.getEmail(), playerProfileEvent.getRegistrationTime(),
                dynamicKeys);
        when(emailApi.sendDayZeroEmail(emailData)).thenThrow(new RuntimeException("Random Runtime Exception"));

        underTest.register(playerProfileEvent);
    }

    @Test
    public void registerShouldNotBubbleUpExceptionIfEmailDataThrowsOne() {

        PlayerProfileEvent playerProfileEvent = buildValidUserProfile();
        playerProfileEvent.setEmail(null);
        underTest.register(playerProfileEvent);
    }

    private PlayerProfileEvent buildValidUserProfile() {
        return new PlayerProfileEvent(new BigDecimal("1234.00"),
                new DateTime(2011, 6, 1, 1, 1, 1, 1),
                "abcdef",
                "the def",
                "wooho",
                "picture",
                "somthing@somwhere.com",
                "US",
                null,
                "VERIFICATION_ID",
                "some provider",
                PlayerProfileStatus.ACTIVE,
                null,
                new DateTime(1980, 6, 1, 1, 1, 1, 1),
                "F",
                null, "someRemoteAddress",
                true,
                "something",
                "G");
    }

}
