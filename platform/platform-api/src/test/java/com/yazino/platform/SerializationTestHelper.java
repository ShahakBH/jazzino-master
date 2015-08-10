package com.yazino.platform;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class SerializationTestHelper {
    public static <T> void testSerializationRoundTrip(T object, boolean checkIfEqual) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(object);
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        @SuppressWarnings({"unchecked"}) T after = (T) oin.readObject();
        if (checkIfEqual)
            assertEquals(object, after);

    }

    public static <T> void testSerializationRoundTrip(T object) throws IOException, ClassNotFoundException {
        testSerializationRoundTrip(object, true);
    }

}
