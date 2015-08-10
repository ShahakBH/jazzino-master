package com.yazino.configuration;

public interface ConfigurationPropertyChangeCallback {

    /**
     * An individual property has changed.
     *
     * @param propertyName  the name of the property that has changed.
     * @param propertyValue the new value.
     */
    void propertyChanged(String propertyName,
                         Object propertyValue);

    /**
     * One or more properties have been changed.
     * <p/>
     * This generally means the properties file has been reloaded.
     */
    void propertiesChanged();


}
