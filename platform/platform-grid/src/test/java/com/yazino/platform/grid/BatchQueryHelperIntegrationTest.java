package com.yazino.platform.grid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BatchQueryHelperIntegrationTest {
    @Autowired(required = true)
    private GigaSpace gigaSpace;

    private BatchQueryTestObject[] objects;

    @Before
    public void setUp() {
        final int numberOfObjects = 5;
        objects = new BatchQueryTestObject[numberOfObjects];
        for (int i = 0; i < numberOfObjects; i++) {
            objects[i] = createTestObject(i);
        }
    }

    @Test
    @Transactional
    public void findByIdReturnsObjectsMatchingTheGivenElements() {
        final BatchQueryHelper<BatchQueryTestObject> unit = new BatchQueryHelper<BatchQueryTestObject>(gigaSpace, "id");
        final Set<BatchQueryTestObject> expected = new HashSet<BatchQueryTestObject>(Arrays.asList(objects[2], objects[3], objects[4]));
        final Set<BatchQueryTestObject> actual = unit.findByIds(BatchQueryTestObject.class, new ArrayList<Object>(Arrays.asList(2, 3, 4)));
        assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findByIdWithAdditionalCriteriaReturnObjectsMatchingTheGivenElements() {
        final BatchQueryHelper<BatchQueryTestObject> unit = new BatchQueryHelper<BatchQueryTestObject>(gigaSpace, "id", "id != 3");
        final Set<BatchQueryTestObject> expected = new HashSet<BatchQueryTestObject>(Arrays.asList(objects[2], objects[4]));
        final Set<BatchQueryTestObject> actual = unit.findByIds(BatchQueryTestObject.class, new ArrayList<Object>(Arrays.asList(2, 4)));
        assertEquals(expected, actual);
    }

    private BatchQueryTestObject createTestObject(final int id) {
        final BatchQueryTestObject obj1 = new BatchQueryTestObject(id);
        gigaSpace.write(obj1);
        return obj1;
    }
}
