package com.yazino.platform.xml;

public final class XMLSerialiser {

    private static final JodaTimeConverter JODA_TIME_CONVERTER = new JodaTimeConverter();
    private static final com.thoughtworks.xstream.XStream XSTREAM = createXStream();

    private XMLSerialiser() {
        // utility class
    }

    public static String toXML(final Object obj) {
        return XSTREAM.toXML(obj);
    }

    private static com.thoughtworks.xstream.XStream createXStream() {
        final com.thoughtworks.xstream.XStream xStream = new com.thoughtworks.xstream.XStream();
        xStream.registerConverter(JODA_TIME_CONVERTER);
        return xStream;
    }

}
