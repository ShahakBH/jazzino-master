package com.yazino.bi.operations.view;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Field annotated with this is meant containing the set of ignored fields
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface IgnoredFieldsDefinition {
}
