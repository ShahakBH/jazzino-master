package com.yazino.test.game;

import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import org.springframework.stereotype.Service;
import com.yazino.platform.service.community.LocationService;

import java.math.BigDecimal;

@Service
public class BlackholeLocationService implements LocationService {
    @Override
    public void notify(final BigDecimal playerId,
                       final BigDecimal sessionId,
                       final LocationChangeType locationChangeType,
                       final Location location) {
    }
}
