package com.yazino.platform.service.community;

import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;

import java.math.BigDecimal;

public interface LocationService {
    void notify(BigDecimal playerId,
                BigDecimal sessionId,
                LocationChangeType locationChangeType,
                Location location);
}
