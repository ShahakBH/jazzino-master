package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.table.TableSummary;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.platform.table.TableService;
import com.yazino.web.domain.LocationDetails;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("locationDetailsRepository")
public class LocationDetailsRepository {
    private final TableService tableService;

    //cglib
    protected LocationDetailsRepository() {
        tableService = null;
    }

    @Autowired
    public LocationDetailsRepository(final TableService tableService) {
        notNull(tableService, "tableService is null");

        this.tableService = tableService;
    }

    @Cacheable(cacheName = "locationDetailsCache")
    public LocationDetails getLocationDetails(final BigDecimal tableId) {
        final TableSummary summary = tableService.findSummaryById(tableId);
        if (summary != null) {
            return new LocationDetails(summary.getId(), summary.getName(), summary.getGameType().getId());
        }
        return null;
    }
}
