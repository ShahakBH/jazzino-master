package com.yazino.platform.xml;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.joda.time.DateTime;

import java.lang.reflect.Constructor;

/**
 * XStream converter for JodaTime values.
 *
 * @see "http://stackoverflow.com/questions/7434242/xstreams-jodatime-local-date-display"
 */
public final class JodaTimeConverter implements Converter {

    @Override
    public boolean canConvert(final Class type) {
        return type != null && DateTime.class.getPackage().equals(type.getPackage());
    }

    @Override
    public void marshal(final Object source, final HierarchicalStreamWriter writer,
                        final MarshallingContext context) {
        writer.setValue(source.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object unmarshal(final HierarchicalStreamReader reader,
                            final UnmarshallingContext context) {
        try {
            final Constructor constructor = context.getRequiredType().getConstructor(Object.class);
            return constructor.newInstance(reader.getValue());

        } catch (final Exception e) {
            throw new RuntimeException(String.format("Exception while deserialising a Joda Time object: %s",
                    context.getRequiredType().getSimpleName()), e);
        }
    }

}