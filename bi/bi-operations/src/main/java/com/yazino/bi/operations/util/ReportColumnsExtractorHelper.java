package com.yazino.bi.operations.util;

import static com.yazino.bi.operations.util.ReportBeanFieldInformationHelper.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.FormatClassSource;
import com.yazino.bi.operations.view.IgnoredFieldsDefinition;
import com.yazino.bi.operations.view.ReportColumnFormat;
import com.yazino.bi.operations.view.ReportField;

/**
 * Helper used to extract columns from a class annotated with ReportField
 */
public final class ReportColumnsExtractorHelper {
    private final List<ReportFieldInformation> fieldsList;
    private Field formatClassSource = null;
    private Set<String> ignoredFieldsNames = new HashSet<String>();

    private static final Map<Class<?>, ReportColumnsExtractorHelper> REPORT_EXTRACTORS =
            new HashMap<Class<?>, ReportColumnsExtractorHelper>();

    /**
     * Resets the list of extractors per class
     */
    public static void reset() {
        REPORT_EXTRACTORS.clear();
    }

    /**
     * Creates or returns from the static storage an extractor helper for the given class
     *
     * @param clazz Class to create the extractor for
     * @return Extractor capable to treat a particular class
     */
    public static ReportColumnsExtractorHelper forClass(final Class<?> clazz) {
        ReportColumnsExtractorHelper reportExtractor = REPORT_EXTRACTORS.get(clazz);
        if (reportExtractor == null) {
            reportExtractor = new ReportColumnsExtractorHelper(clazz);
            REPORT_EXTRACTORS.put(clazz, reportExtractor);
        }
        return reportExtractor;
    }

    /**
     * Creates and initializes the extractor
     *
     * @param reportClass Class representing a single report line
     */
    private ReportColumnsExtractorHelper(final Class<?> reportClass) {
        final List<Field> initialList = extractInitialList(reportClass);
        this.fieldsList = buildFieldList(initialList, reportClass);
    }

    /**
     * Builds a list of developed fields information beans from the initial list of annotations
     *
     * @param initialList List of annotated fields
     * @param reportClass Class for the individual report line
     * @return List to use to extract the fields information
     */
    private List<ReportFieldInformation> buildFieldList(final List<Field> initialList,
            final Class<?> reportClass) {
        final List<ReportFieldInformation> list = new ArrayList<ReportFieldInformation>();
        for (final Field field : initialList) {
            if (ignoredFieldsNames.contains(field.getName())) {
                continue;
            }
            final ReportField formatDefinition = field.getAnnotation(ReportField.class);
            final String multiSource = formatDefinition.multipleColumnsSource();
            if (!"".equals(multiSource)) {
                final List<String> multiList = getStaticFieldValue(multiSource, reportClass);
                if (multiList != null) {
                    int pos = 0;
                    for (final String header : multiList) {
                        list.add(new ReportFieldInformation(field, null, header, formatDefinition.format(),
                                formatDefinition.columnWidth(), formatDefinition.customFormats(), pos++));
                    }
                }
            } else {
                list.add(new ReportFieldInformation(field, null, formatDefinition.header(), formatDefinition
                        .format(), formatDefinition.columnWidth(), formatDefinition.customFormats(), -1));
            }
        }
        return list;
    }

    /**
     * Creates the list of fields that doesn't take into account
     * the eventual variable columns
     *
     * @param reportClass Class for the individual report line
     * @return Initial list of fields
     */
    private List<Field> extractInitialList(final Class<?> reportClass) {
        final List<Field> list = new ArrayList<Field>();
        for (final Field field : reportClass.getDeclaredFields()) {
            final FormatClassSource sourceAnnotation = field.getAnnotation(FormatClassSource.class);
            if (sourceAnnotation != null) {
                formatClassSource = field;
            } else if (field.getAnnotation(IgnoredFieldsDefinition.class) != null) {
                try {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    final Set<String> ignores = (Set<String>) field.get(null);
                    if (ignores != null) {
                        ignoredFieldsNames.clear();
                        for (final String ignore : ignores) {
                            ignoredFieldsNames.add(ignore);
                        }
                    }
                } catch (final Exception e) {
                    // We simply ignore all the bad declatations
                }
            }
            if (field.getAnnotation(ReportField.class) == null) {
                continue;
            }
            list.add(field);
        }
        // Sort by order of annotations
        Collections.sort(list, new Comparator<Field>() {
            @Override
            public int compare(final Field o1, final Field o2) {
                final ReportField format1 = o1.getAnnotation(ReportField.class);
                final ReportField format2 = o2.getAnnotation(ReportField.class);
                if (format1.position() > format2.position()) {
                    return 1;
                } else {
                    if (format1.position() < format2.position()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }

        });
        return list;
    }

    /**
     * Lists the metainformation for all the report columns, based on the class annotations
     *
     * @return List of columns metadata
     */
    public List<ReportFieldInformation> getFieldsList() {
        return fieldsList;
    }

    /**
     * Extracts a name-to-fieldinfo map for a given bean
     *
     * @param bean Source report line bean
     * @return Map of field information including field values
     */
    public Map<String, ReportFieldInformation> getFields(final Object bean) {
        final Map<String, ReportFieldInformation> fields =
                new LinkedHashMap<String, ReportFieldInformation>();
        for (final ReportFieldInformation fieldInfo : fieldsList) {
            final int relativePosition = fieldInfo.getRelativePosition();

            final Object fieldValue;
            final String referenceName;
            if (relativePosition >= 0) {
                final List<?> fieldList = getFieldValue(fieldInfo.getField(), bean);
                if (fieldList == null) {
                    fieldValue = null;
                } else {
                    fieldValue = fieldList.get(relativePosition);
                }
                referenceName = fieldInfo.getField().getName() + "." + relativePosition;
            } else {
                fieldValue = getFieldValue(fieldInfo.getField(), bean);
                referenceName = fieldInfo.getField().getName();
            }
            final ReportColumnFormat format =
                    extractFieldFormat(bean, fieldInfo.getFormat(), fieldInfo.getCustomFormats());

            final ReportFieldInformation filledField =
                    new ReportFieldInformation(null, fieldValue, fieldInfo.getFieldHeader(), format,
                            fieldInfo.getColumnWidth(), null, relativePosition);

            fields.put(referenceName, filledField);
        }
        return fields;
    }

    /**
     * Determines the format to apply to the field
     *
     * @param bean          Bean the field is extracted from
     * @param format        Default field format
     * @param customFormats List of custom format definitions
     * @return Format to apply
     */
    private ReportColumnFormat extractFieldFormat(final Object bean,
            final ReportColumnFormat format,
            final CustomColumnFormat[] customFormats) {
        if (customFormats.length == 0 || formatClassSource == null) {
            return format;
        }
        final String customFormatClass = getFieldValue(formatClassSource, bean);
        if (customFormatClass == null) {
            return format;
        }
        for (final CustomColumnFormat customFormat : customFormats) {
            if (customFormatClass.equals(customFormat.value())) {
                return customFormat.format();
            }
        }
        return format;
    }
}
