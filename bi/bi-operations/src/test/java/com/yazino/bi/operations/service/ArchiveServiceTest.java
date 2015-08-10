package com.yazino.bi.operations.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.PromotionMaintenanceService;
import com.yazino.bi.operations.persistence.JdbcBackOfficePromotionDao;

import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static com.yazino.bi.operations.service.ArchiveService.DAYS_BEFORE_ARCHIVING;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
    @Mock
    private JdbcBackOfficePromotionDao promotionDao;

    @Mock
    private PromotionMaintenanceService promotionMaintenanceService;

    private ArchiveService archiveService;

    @Before
    public void init() {
        archiveService = new ArchiveService(promotionMaintenanceService, promotionDao);
    }

    @Test
    public void shouldArchiveExpiredPromotions() {
        // GIVEN the following expired promotions
        given(promotionDao.findPromotionsOlderThan(DAYS_BEFORE_ARCHIVING)).willReturn(Arrays.asList(3l, 1l, 2l));

        // WHEN archiving expired promotions
        archiveService.archiveExpiredPromotions();

        // THEN all expired promos should be archived
        verify(promotionDao).archivePromotion(3l);
        verify(promotionDao).archivePromotion(1l);
        verify(promotionDao).archivePromotion(2l);
    }

    @Test
    public void shouldDeletePromotionAfterPromotionIsArchived() {
        // GIVEN the following expired promotions
        given(promotionDao.findPromotionsOlderThan(DAYS_BEFORE_ARCHIVING)).willReturn(Arrays.asList(3l));

        // WHEN archiving expired promotions
        archiveService.archiveExpiredPromotions();

        // THEN all expired promo should be deleted AFTER it has been archived
        InOrder inOrder = inOrder(promotionDao, promotionMaintenanceService);
        inOrder.verify(promotionDao).archivePromotion(3l);
        inOrder.verify(promotionMaintenanceService).delete(3l);
    }
}
