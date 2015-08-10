package com.yazino.platform.service.community;

import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.table.TableType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LocationServiceTest {
    private static final BigDecimal OWNER_ID = BigDecimal.TEN;
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(234);

    @Mock
    private GigaSpace globalGigaSpace;

    private LocationService underTest;

    @Before
    public void setUp() {
        underTest = new GigaSpaceLocationService(globalGigaSpace);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullGigaSpace() {
        new GigaSpaceLocationService(null);
    }

    @Test(expected = NullPointerException.class)
    public void aLocationChangeNotificationRejectsANullPlayerId() {
        underTest.notify(null, SESSION_ID, LocationChangeType.ADD, aLocation());
    }

    @Test
    public void aLocationChangeNotificationAcceptsANullSessionId() {
        underTest.notify(PLAYER_ID, null, LocationChangeType.ADD, aLocation());
    }

    @Test(expected = NullPointerException.class)
    public void aLocationChangeNotificationRejectsANullLocationChangeType() {
        underTest.notify(PLAYER_ID, SESSION_ID, null, aLocation());
    }

    @Test(expected = NullPointerException.class)
    public void aLocationChangeNotificationRejectsANullLocation() {
        underTest.notify(PLAYER_ID, SESSION_ID, LocationChangeType.REMOVE, null);
    }

    @Test
    public void aLocationChangeNotificationCausesARequestToBeWrittenToTheSpace() {
        underTest.notify(PLAYER_ID, SESSION_ID, LocationChangeType.REMOVE, aLocation());

        verify(globalGigaSpace).write(new LocationChangeNotification(PLAYER_ID, SESSION_ID, LocationChangeType.REMOVE, aLocation()));
    }

    private Location aLocation() {
        return new Location("aLocationId", "aLocationName", "aGameType", OWNER_ID, TableType.PRIVATE);
    }

}
