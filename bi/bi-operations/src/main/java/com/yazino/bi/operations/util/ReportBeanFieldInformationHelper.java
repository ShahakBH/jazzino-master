package com.yazino.bi.operations.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Simplifies basic information recovery from the report beans
 */
public final class ReportBeanFieldInformationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ReportBeanFieldInformationHelper.class);

    /**
     * No public constructor
     */
    private ReportBeanFieldInformationHelper() {
    }

    /**
     * Gets the value of a given static field
     *
     * @param beanClass Class of the bean we're looking at
     * @param fieldName Name of the field we're working with
     * @param <T>       Class to convert the return value to
     * @return Field information or null if there is no way to obtain it
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStaticFieldValue(final String fieldName, final Class<?> beanClass) {
        try {
            final Field field = beanClass.getDeclaredField(fieldName);
            return (T) getFieldValue(field, fieldName, null, beanClass);
        } catch (final Throwable t) {
            LOG.warn("Unable to get static field value, returning null", t);
            return null;
        }
    }

    /**
     * Gets the value of a given field
     *
     * @param field Field to work with
     * @param bean  Bean itself. Should not be null
     * @param <T>   Class to convert the return value to
     * @return Field information or null if there is no way to obtain it
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(final Field field, final Object bean) {
        try {
            final String fieldName = field.getName();
            final Class<?> beanClass = bean.getClass();
            return (T) getFieldValue(field, fieldName, bean, beanClass);
        } catch (final Throwable t) {
            LOG.warn("Unable to get field value, returning null", t);
            return null;
        }
    }

    /**
     * Gets the value of a given field
     *
     * @param field     Field to work with
     * @param beanClass Class of the bean we're looking at
     * @param fieldName Name of the field we're working with
     * @param bean      Bean itself. If null, we're just getting the annotation info, except if the field is static
     * @param <T>       Class to convert the return value to
     * @return Field information or null if there is no way to obtain it
     * @throws IllegalAccessException    .
     * @throws InvocationTargetException .
     * @throws NoSuchMethodException     .
     */
    private static <T> T getFieldValue(final Field field, final String fieldName, final Object bean,
                                       final Class<?> beanClass)
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        if (bean == null && !Modifier.isStatic(field.getModifiers())) {
            LOG.warn("Unable to get field value from bean, returning null");
            return null;
        }

        final String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        @SuppressWarnings("unchecked")
        final T fieldValue = (T) beanClass.getMethod(getterName).invoke(bean);
        return fieldValue;
    }
}
