package com.yazino.promotions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@ContextConfiguration
@Transactional
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class PromotionChipsPercentageMysqlDaoIntegrationTest {

    public static final long PROMOTION_DEFINITION_ID = 100L;
    @Autowired
    private PromotionChipsPercentageDao underTest;

    @Test
    public void saveShouldAddPackagesAndCorrespondingPercentagesToDb() {

        Map<Integer, BigDecimal> chipsPackagePercentages = newLinkedHashMap();
        chipsPackagePercentages.put(2, new BigDecimal("10.00"));
        underTest.save(PROMOTION_DEFINITION_ID, chipsPackagePercentages);

        Map<Integer, BigDecimal> chipsPercentages = underTest.getChipsPercentages(PROMOTION_DEFINITION_ID);
        assertThat(chipsPercentages, is(chipsPackagePercentages));

    }

    @Test
    public void updateShouldReplaceRecordsIfAlreadyPresent() {
        Map<Integer, BigDecimal> chipsPackagePercentages = newLinkedHashMap();
        chipsPackagePercentages.put(2, new BigDecimal("10.00"));
        underTest.save(PROMOTION_DEFINITION_ID, chipsPackagePercentages);


        Map<Integer, BigDecimal> updatedChipsPackagePercentages = newLinkedHashMap();
        updatedChipsPackagePercentages.put(2, new BigDecimal("20.00"));
        underTest.updateChipsPercentage(PROMOTION_DEFINITION_ID, updatedChipsPackagePercentages);


        Map<Integer, BigDecimal> chipsPercentagesFromDb = underTest.getChipsPercentages(PROMOTION_DEFINITION_ID);
        assertThat(chipsPercentagesFromDb, is(updatedChipsPackagePercentages));
    }

    @Test
    public void updateShouldInsertRecordsIfNotAlreadyPresent() {

        Map<Integer, BigDecimal> chipsPackagePercentages = newLinkedHashMap();
        chipsPackagePercentages.put(2, new BigDecimal("30.00"));
        underTest.updateChipsPercentage(PROMOTION_DEFINITION_ID, chipsPackagePercentages);


        Map<Integer, BigDecimal> chipsPercentagesFromDb = underTest.getChipsPercentages(PROMOTION_DEFINITION_ID);
        assertThat(chipsPercentagesFromDb, is(chipsPackagePercentages));
    }
}
