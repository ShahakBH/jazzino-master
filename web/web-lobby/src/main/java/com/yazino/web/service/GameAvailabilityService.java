package com.yazino.web.service;

import com.yazino.platform.table.CountdownService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("gameAvailabilityService")
public class GameAvailabilityService {

    public enum Availability { AVAILABLE, MAINTENANCE_SCHEDULED, DISABLED }

    private TableLobbyService tableService;
    private CountdownService countdownService;

    @Autowired(required = true)
    public GameAvailabilityService(CountdownService countdownService,
                                   TableLobbyService tableService) {
        this.countdownService = countdownService;
        this.tableService = tableService;
    }

    private Long getMaintenanceStartsAtMillis(String gameType) {
        Map<String,Long> countdowns = countdownService.findAll();
        Long earliest = null;
        for (String countdownId : countdowns.keySet()) {
            if ("ALL".equals(countdownId) || gameType.equalsIgnoreCase(countdownId)) {
                Long startsAtMillis = countdowns.get(countdownId);
                if (earliest == null || startsAtMillis < earliest) {
                    earliest = startsAtMillis;
                }
            }
        }
        return earliest;
    }

    public GameAvailability getAvailabilityOfGameType(String gameType) {
        Availability availability = Availability.AVAILABLE;
        Long maintenanceStartsAtMillis = null;
        if (!tableService.isGameTypeAvailable(gameType)) {
             availability = Availability.DISABLED;
        } else {
            maintenanceStartsAtMillis = getMaintenanceStartsAtMillis(gameType);
            if (maintenanceStartsAtMillis != null) {
                availability = Availability.MAINTENANCE_SCHEDULED;
            }
        }
        return new GameAvailability(availability, maintenanceStartsAtMillis);
    }

}
