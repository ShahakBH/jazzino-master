package com.yazino.spring.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks the given value as suitable for public access.
 * <p/>
 * The URL patterns to use may be either specified in the value element, or
 * in a {@link org.springframework.web.bind.annotation.RequestMapping} annotation
 * on the same element. You'll need to use the direct value if you want
 * full Ant pathing.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowPublicAccess {

    String[] value() default {};

}
