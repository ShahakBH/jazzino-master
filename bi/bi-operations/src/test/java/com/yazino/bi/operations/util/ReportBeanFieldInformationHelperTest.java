package com.yazino.bi.operations.util;

import static org.junit.Assert.*;
import static strata.server.test.helpers.classes.ClassStructureTestSupport.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class ReportBeanFieldInformationHelperTest {
    @Test
    public void shouldHaveDefaultConstructor() throws IllegalAccessException, InvocationTargetException,
            InstantiationException {
        // GIVEN report bean class

        // WHEN calling the constructor
        final boolean result = testDefaultConstructor(ReportBeanFieldInformationHelper.class);

        // THEN the result is success
        assertTrue(result);
    }
}
