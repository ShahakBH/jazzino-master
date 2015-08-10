package com.yazino.platform.model;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class SerializationTestHelper {

    public static <T> void testSerializationRoundTrip(T object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(object);
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        @SuppressWarnings({"unchecked"}) T after = (T) oin.readObject();
        assertEquals(object, after);
    }
}
