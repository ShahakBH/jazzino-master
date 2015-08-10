package com.yazino.platform.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.intersection;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")

public class JDBCSequenceGeneratorIntegrationTest {
    private static final int BATCH_SIZE = 100;

    @Autowired
    @Qualifier("sequenceGenerator")
    private SequenceGenerator sequenceGenerator;

    @Autowired
    @Qualifier("anotherSequenceGenerator")
    private SequenceGenerator anotherSequenceGenerator;

    @Test
    @Transactional
    public void theSequenceGeneratorShouldReturnUniqueValues() {
        final BigDecimal firstItem = sequenceGenerator.next();
        final BigDecimal secondItem = sequenceGenerator.next();

        assertThat(firstItem, is(not(nullValue())));
        assertThat(firstItem, is(not(equalTo(secondItem))));
    }

    @Test
    @Transactional
    public void batchesOfKeysAreIndependent() {
        final Set<BigDecimal> firstSequence = allItemsInBatch(sequenceGenerator);
        final Set<BigDecimal> secondSequence = allItemsInBatch(anotherSequenceGenerator);

        assertThat(intersection(firstSequence, secondSequence).size(), is(equalTo(0)));
    }

    @Test
    @Transactional
    public void aGroupOfKeysCanBeRequested() {
        final Set<BigDecimal> keys = sequenceGenerator.next(BATCH_SIZE * 3 + 1);

        assertThat(keys.size(), is(equalTo(BATCH_SIZE * 3 + 1)));
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    public void aGroupOfZeroKeysThrowsAnIllegalArgumentException() {
        sequenceGenerator.next(0);
    }

    @Test
    @Transactional
    public void batchesAreRefreshWhenExhausted() {
        final Set<BigDecimal> firstBatch = allItemsInBatch(sequenceGenerator);
        final Set<BigDecimal> secondBatch = allItemsInBatch(sequenceGenerator);

        assertThat(intersection(firstBatch, secondBatch).size(), is(equalTo(0)));
    }

    @Test
    @Transactional
    public void batchedValuesShouldBeUsedUntilBatchIsExhausted() {
        final Set<BigDecimal> keysSoFar = new HashSet<>();
        for (int i = 0; i < BATCH_SIZE; ++i) {
            final BigDecimal currentItem = sequenceGenerator.next();
            assertThat(keysSoFar, not(hasItem(currentItem)));
            keysSoFar.add(currentItem);
        }
        assertThat(keysSoFar.size(), is(equalTo(BATCH_SIZE)));
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void theGeneratorCannotBeCreatedWithANullTemplate() {
        new JDBCSequenceGenerator(null, BATCH_SIZE);
    }

    private Set<BigDecimal> allItemsInBatch(final SequenceGenerator sequenceGenerator) {
        final Set<BigDecimal> batch = new HashSet<>();
        for (int i = 0; i < BATCH_SIZE; ++i) {
            batch.add(sequenceGenerator.next());
        }
        return batch;
    }
}
