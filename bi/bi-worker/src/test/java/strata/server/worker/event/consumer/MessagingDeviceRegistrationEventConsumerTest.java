package strata.server.worker.event.consumer;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.Platform;
import com.yazino.platform.android.MessagingDeviceRegistrationEvent;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class MessagingDeviceRegistrationEventConsumerTest {

    private final static String DEVICE_TOKEN = "DEVICE_TOKEN";
    private final static BigDecimal PLAYER_ID = BigDecimal.valueOf(141);
    private final static String GAME_TYPE = "GAME_TYPE";
    private final static String REGISTRATION_ID = "REGISTRATION_ID";

    private MobileDeviceService mobileDeviceDao = mock(MobileDeviceService.class);
    private MessagingDeviceRegistrationEventConsumer underTest = new MessagingDeviceRegistrationEventConsumer(mobileDeviceDao);

    @Test
    public void handleShouldPersistRegistrationId() {
        MessagingDeviceRegistrationEvent message = sampleMessage();

        underTest.handle(message);

        verify(mobileDeviceDao).register(PLAYER_ID, GAME_TYPE, Platform.ANDROID, null, null, REGISTRATION_ID);
    }

    @Test
    public void shouldLogButNotPropagateExceptions() {
        ListAppender appender = ListAppender.addTo(MessagingDeviceRegistrationEventConsumer.class);
        doThrow(new RuntimeException("Some Exception"))
                .when(mobileDeviceDao).register(any(BigDecimal.class), anyString(), any(Platform.class), anyString(), anyString(), anyString());
        MessagingDeviceRegistrationEvent message = sampleMessage();

        underTest.handle(message);

//        assertThat(appender.getMessages(), (Matcher) hasItem(StringStartsWith.startsWith("Unable to handle device registration message"))); // TODO fix class cast exception

        /* Why the cast to Matcher? see http://stackoverflow.com/questions/1092981/hamcrests-hasitems */
    }

    private MessagingDeviceRegistrationEvent sampleMessage() {
        return new MessagingDeviceRegistrationEvent(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, Platform.ANDROID);
    }

}
