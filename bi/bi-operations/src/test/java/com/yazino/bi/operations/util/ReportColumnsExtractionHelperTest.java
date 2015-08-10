package com.yazino.bi.operations.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ReportColumnsExtractionHelperTest {
    @Before
    public void init() {
        ReportColumnsExtractorHelper.reset();
    }

    @Test
    public void shouldInitializeExtractorAndReturnCorrectColumnModel() {
        // GIVEN the headers information for the multiple field

        final List < String > hs = new ArrayList < String >();
        hs.add("H1");
        hs.add("H2");
        ReportTestBean.setHeaders(hs);

        // AND the report columns extractor initialized with the test class
        final ReportColumnsExtractorHelper extractor = ReportColumnsExtractorHelper.forClass(ReportTestBean.class);

        // WHEN requesting the columns information list
        final List < ReportFieldInformation > headers = extractor.getFieldsList();

        // THEN returns the correct list of column models
        assertEquals("Header", headers.get(0).getFieldHeader());
        assertEquals("Header NG", headers.get(1).getFieldHeader());
        assertEquals("H", headers.get(2).getFieldHeader());
        assertEquals("H1", headers.get(3).getFieldHeader());
        assertEquals("H2", headers.get(4).getFieldHeader());
        assertEquals("Header", headers.get(5).getFieldHeader());
    }

    @Test
    @Ignore("Intermittent failures due to non-deterministic ordering of fields stored in set")
    public void shouldCorrectlyExtractFields() {
        // GIVEN the report columns extractor initialized with the test class
        final ReportColumnsExtractorHelper extractor = ReportColumnsExtractorHelper.forClass(ReportTestBean.class);

        // AND a bean to extract the information from
        final ReportTestBean bean = new ReportTestBean();
        bean.setStringField("str");
        bean.setIntegerField(7);
        bean.setStringField2("str2");
        final List < String > multiSource = new ArrayList < String >();
        multiSource.add("m1");
        multiSource.add("m2");
        bean.setMultipleField(multiSource);

        // WHEN requesting the field information map
        final Map < String, ReportFieldInformation > fieldInfoMap = extractor.getFields(bean);

        // THEN the returned map contains all the needed information
        assertEquals("str", fieldInfoMap.get("stringField").getFieldValue());
        assertEquals(7, fieldInfoMap.get("integerField").getFieldValue());
        assertNull(fieldInfoMap.get("fieldWithNoGetter").getFieldValue());
        assertEquals("str2", fieldInfoMap.get("stringField2").getFieldValue());

        ReportFieldInformation nThField = null;
        final Iterator < ReportFieldInformation > iterator = fieldInfoMap.values().iterator();
        for (int i = 0; i < 4; i++) {
            nThField = iterator.next();
        }
        assertEquals("m1", nThField.getFieldValue());
        nThField = iterator.next();
        assertEquals("m2", nThField.getFieldValue());
        nThField = iterator.next();
        assertEquals("str2", nThField.getFieldValue());
    }
}
