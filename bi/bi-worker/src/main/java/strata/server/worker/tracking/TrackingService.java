package strata.server.worker.tracking;

import com.yazino.bi.tracking.TrackingDao;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class TrackingService {

    private final TrackingDao trackingDao;

    @Autowired
    public TrackingService(TrackingDao trackingDao) {
        this.trackingDao = trackingDao;
    }

    public void track(Platform platform, BigDecimal playerId, String name, Map<String, String> properties, DateTime received) {
        trackingDao.save(platform, playerId, name, properties, received);
    }
}
