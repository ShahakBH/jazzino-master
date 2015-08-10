package strata.server.worker.tracking;

import com.yazino.bi.tracking.TrackingDao;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TrackingServiceTest {

    private final static Platform PLATFORM = Platform.WEB;
    private final static BigDecimal PLAYER_ID = BigDecimal.valueOf(141);
    private final static String NAME = "sample-event";
    private final static Map<String, String> PROPERTIES = new HashMap<String, String>();
    private static final DateTime RECEIVED = new DateTime("2012-12-12T23:00:00Z");

    private TrackingDao trackingDao = mock(TrackingDao.class);
    private TrackingService underTest = new TrackingService(trackingDao);

    @Test
    public void trackShouldPersistEvent() {
        underTest.track(PLATFORM, PLAYER_ID, NAME, PROPERTIES, RECEIVED);

        verify(trackingDao).save(PLATFORM, PLAYER_ID, NAME, PROPERTIES, RECEIVED);
    }
}
