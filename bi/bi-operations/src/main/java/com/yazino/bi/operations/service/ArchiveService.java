package com.yazino.bi.operations.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.PromotionMaintenanceService;
import com.yazino.bi.operations.persistence.JdbcBackOfficePromotionDao;

import java.util.List;

@Service("promotionArchiveService")
public class ArchiveService {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveService.class);

    static final int DAYS_BEFORE_ARCHIVING = 28;

    private final JdbcBackOfficePromotionDao jdbcBackOfficePromotionDao;
    private PromotionMaintenanceService promotionMaintenanceService;

    @Autowired
    public ArchiveService(
            @Qualifier("promotionMaintenanceService") final PromotionMaintenanceService promotionMaintenanceService,
            final JdbcBackOfficePromotionDao jdbcBackOfficePromotionDao) {
        this.promotionMaintenanceService = promotionMaintenanceService;
        this.jdbcBackOfficePromotionDao = jdbcBackOfficePromotionDao;
    }

    public void archiveExpiredPromotions() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Archiving expired promotions");
        }
        final List<Long> expiredPromotions = jdbcBackOfficePromotionDao.findPromotionsOlderThan(DAYS_BEFORE_ARCHIVING);
        for (Long expiredPromotion : expiredPromotions) {
            try {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Archiving promotion: " + expiredPromotion);
                }
                jdbcBackOfficePromotionDao.archivePromotion(expiredPromotion);
                promotionMaintenanceService.delete(expiredPromotion);
            } catch (Exception e) {
                LOG.error("Failed to archive promotion: " + expiredPromotion, e);
            }
        }
    }
}
