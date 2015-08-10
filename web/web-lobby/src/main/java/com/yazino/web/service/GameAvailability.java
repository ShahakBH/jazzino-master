package com.yazino.web.service;

import static com.google.common.base.Preconditions.checkArgument;

public class GameAvailability {
    private final GameAvailabilityService.Availability availability;
    private final Long maintenanceStartsAtMillis;

    public GameAvailability(GameAvailabilityService.Availability availability) {
        this(availability, null);
    }

    public GameAvailability(GameAvailabilityService.Availability availability, Long maintenanceStartsAtMillis) {
        checkArgument(availability != GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED || maintenanceStartsAtMillis != null,
                "Maintenance countdown must be provided when availability is MAINTENANCE_SCHEDULED");
        this.availability = availability;
        this.maintenanceStartsAtMillis = maintenanceStartsAtMillis;
    }

    public GameAvailabilityService.Availability getAvailability() {
        return availability;
    }

    public Long getMaintenanceStartsAtMillis() {
        return maintenanceStartsAtMillis;
    }
}
