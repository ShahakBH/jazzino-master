package com.yazino.bi.operations.view;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The field annotated with this will become the source of the custom classes for the field format
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface FormatClassSource {
}
