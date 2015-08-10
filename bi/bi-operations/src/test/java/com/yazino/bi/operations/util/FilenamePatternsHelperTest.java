package com.yazino.bi.operations.util;

import org.junit.Test;
import strata.server.test.helpers.classes.ClassStructureTestSupport;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.yazino.bi.operations.util.FilenamePatternsHelper.treatFileName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilenamePatternsHelperTest {

    @Test
    public void shouldTreatFileName() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        assertEquals("file" + format.format(new Date()) + ".csv", treatFileName("file#.csv"));
        assertEquals("any", treatFileName("any"));
    }

    @Test
    public void shouldHaveHiddenConstructor() throws IllegalAccessException, InvocationTargetException,
            InstantiationException {
        assertTrue(ClassStructureTestSupport.testDefaultConstructor(FilenamePatternsHelper.class));
    }
}
