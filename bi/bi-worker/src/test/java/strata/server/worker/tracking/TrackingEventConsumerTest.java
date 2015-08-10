package strata.server.worker.tracking;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.Platform;
import com.yazino.platform.tracking.TrackingEvent;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringStartsWith;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;

public class TrackingEventConsumerTest {

    private final static Platform PLATFORM = Platform.WEB;
    private final static BigDecimal PLAYER_ID = BigDecimal.valueOf(141);
    private final static String NAME = "sample-event";
    private final static Map<String, String> PROPERTIES = new HashMap<String, String>();
    private static final DateTime RECEIVED = new DateTime("2012-12-12T23:00:00Z");

    private TrackingService trackingService = mock(TrackingService.class);
    private TrackingEventConsumer underTest = new TrackingEventConsumer(trackingService);

    @Test
    public void handleInvokeTrackingService() {
        TrackingEvent event = sampleEvent();

        underTest.handle(event);

        verify(trackingService).track(PLATFORM, PLAYER_ID, NAME, PROPERTIES, RECEIVED);
    }

    @Test
    public void shouldLogButNotPropagateExceptions() {
        ListAppender appender = ListAppender.addTo(TrackingEventConsumer.class);
        doThrow(new RuntimeException("Some Exception"))
                .when(trackingService).track(any(Platform.class), any(BigDecimal.class), anyString(), anyMap(), any(DateTime.class));

        TrackingEvent event = sampleEvent();

        underTest.handle(event);

        assertThat(appender.getMessages(), (Matcher) hasItem(StringStartsWith.startsWith("Unable to handle TrackingEvent"))); // TODO fix class cast exception

        /* Why the cast to Matcher? see http://stackoverflow.com/questions/1092981/hamcrests-hasitems */
    }

    private TrackingEvent sampleEvent() {
        return new TrackingEvent(PLATFORM, PLAYER_ID, NAME, PROPERTIES, RECEIVED);
    }

}
