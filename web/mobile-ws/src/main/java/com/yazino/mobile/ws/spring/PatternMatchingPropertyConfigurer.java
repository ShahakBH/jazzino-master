package com.yazino.mobile.ws.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link PropertyPlaceholderConfigurer} which is used to populate instances of {@link PatternMatchingPropertyMap}'s,
 * based on that objects pattern.
 * e.g.
 *   <bean id="iosMinimumVersions" class="com.yazino.mobile.ws.spring.PatternMatchingPropertyMap">
 *       <constructor-arg value="facebook\.(.*)\.application\.id"/>
 *   </bean>
 * All properties that match that pattern will now exist in iosMinimumVersions using the group as the key.
 */
public class PatternMatchingPropertyConfigurer extends PropertyPlaceholderConfigurer implements BeanPostProcessor {

    private Properties mProperties = new Properties();

    @Override
    protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties props)
            throws BeansException {
        mProperties = props;
        super.processProperties(beanFactoryToProcess, props);
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean != null && bean.getClass().equals(PatternMatchingPropertyMap.class)) {
            PatternMatchingPropertyMap source = (PatternMatchingPropertyMap) bean;
            Pattern pattern = source.getPattern();
            for (String key : mProperties.stringPropertyNames()) {
                Matcher matcher = pattern.matcher(key);
                if (matcher.matches()) {
                    String mapKey = matcher.group(0);
                    if (matcher.groupCount() > 0) {
                        mapKey = matcher.group(1);
                    }
                    String mapValue = mProperties.getProperty(key);
                    source.put(mapKey, mapValue);
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    /**
     * Used for tests.
     * @param properties test properties
     */
    final void setTestProperties(final Properties properties) {
        mProperties = properties;
    }
}
