package com.yazino.novomatic;

import java.io.*;

public class FakeNovomaticStateSerialization {

    public byte[] toBytes(FakeNovomaticState fakeNovomaticInternalState) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(fakeNovomaticInternalState);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize", e);
        }
    }

    public FakeNovomaticState fromBytes(final byte[] bytes) {
        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            final ObjectInputStream stream = new ObjectInputStream(in);
            return (FakeNovomaticState) stream.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize", e);
        }
    }
}

