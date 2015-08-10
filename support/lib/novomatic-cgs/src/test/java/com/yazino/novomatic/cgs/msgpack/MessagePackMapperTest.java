package com.yazino.novomatic.cgs.msgpack;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"unchecked", "MismatchedQueryAndUpdateOfCollection"})
public class MessagePackMapperTest {
    private MessagePackMapper underTest;


    @Before
    public void setUp() {
        underTest = new MessagePackMapper();
    }

    @Test
    public void shouldDeserializeBasicMap() throws IOException {
        final Map<String, Object> original = new HashMap<String, Object>();
        original.put("k1", "v1");
        testSerializationRoundtrip(original, Map.class);
    }

    @Test
    public void shouldDeserializeMapWithList() throws IOException {
        final Map<String, List<String>> original = new HashMap<String, List<String>>();
        original.put("k1", Arrays.asList("v1", "v2", "v3"));
        testSerializationRoundtrip(original, Map.class);
    }

    private void testSerializationRoundtrip(Object original, Class clazz) throws IOException {
        byte[] pack = underTest.write(original);
        final Object actual = underTest.read(pack, clazz);
        assertEquals(original, actual);
    }

    @Test
    public void shouldDeserializeMapOfMaps() throws IOException {
        final Map<String, Map<String, String>> original = new HashMap<String, Map<String, String>>();
        final Map<String, String> secondLayer = new HashMap<String, String>();
        secondLayer.put("l2k1", "l2v2");
        original.put("k2", secondLayer);
        testSerializationRoundtrip(original, Map.class);
    }

    @Test
    public void shouldDeserializeSimpleList() throws IOException {
        final List<String> original = Arrays.asList("v1", "v2", "v3");
        testSerializationRoundtrip(original, List.class);
    }

    @Test
    public void shouldDeserialiseMapContainingByteArray() throws IOException {
        Map<String, Object> testMap = new HashMap<String, Object>();
        final int[] value = {0, 0, 0, 0, 220, 0, 0, 0, 89, 9, 76, 62, 245, 95, 1, 0, 156, 0, 0, 0, 28, 0, 0, 0, 2, 0, 0, 0, 0,
                0, 0, 0, 156, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 152, 0, 0, 0, 15, 0, 0, 0, 2, 0, 0, 0, 112, 0, 0, 0,
                232, 3, 0, 0, 16, 39, 0, 0, 9, 0, 0, 0, 1, 0, 0, 0, 10, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 5, 0, 0, 0,
                7, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0, 10, 0,
                0, 0, 20, 0, 0, 0, 50, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 20, 0, 0, 0, 50, 0, 0, 0,
                100, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 32, 0, 0, 0, 5, 0, 0, 0, 87, 82, 71, 0, 87, 66, 71, 0, 87,
                66, 71, 0, 82, 69, 76, 0, 82, 67, 76, 0, 16, 0, 0, 0, 15, 0, 0, 0, 10, 0, 0, 0, 69, 69, 69, 69,
                69, 69, 69, 69, 69, 69, 0, 0, 1, 0, 0, 0, 4, 0, 0, 0, 16, 39, 0, 0};
        testMap.put("k", toBytes(value));
        final byte[] write = underTest.write(testMap);
        final Map fromBytes = underTest.read(write, Map.class, "k");
        assertArrayEquals((byte[]) testMap.get("k"), (byte[]) fromBytes.get("k"));
    }

    @Test
    public void shouldDeserialiseMoreComplexMap() throws IOException {
        //{type="rsp_gmengine_list_descr", games=[{"type":"game_descr","id":90101,"version":"1.0","name":"Just Jewels","pop":96.0}]}
        int[] payload = new int[]{130, 164, 116, 121, 112, 101, 183, 114, 115, 112, 95, 103, 109, 101, 110, 103, 105, 110, 101, 95, 108, 105, 115, 116, 95, 100, 101, 115, 99, 114, 165, 103, 97, 109, 101, 115, 145, 133, 164, 116, 121, 112, 101, 170, 103, 97, 109, 101, 95, 100, 101, 115, 99, 114, 162, 105, 100, 206, 0, 1, 95, 245, 167, 118, 101, 114, 115, 105, 111, 110, 163, 49, 46, 48, 164, 110, 97, 109, 101, 171, 74, 117, 115, 116, 32, 74, 101, 119, 101, 108, 115, 163, 112, 111, 112, 203, 64, 88, 0, 0, 0, 0, 0, 0};
        Map payloadAsMap = new HashMap();
        payloadAsMap.put("type", "rsp_gmengine_list_descr");
        Map game = new HashMap();
        game.put("type", "game_descr");
        game.put("id", 90101l);
        game.put("version", "1.0");
        game.put("name", "Just Jewels");
        game.put("pop", 96.0d);
        payloadAsMap.put("games", Arrays.asList(game));
        byte[] raw = toBytes(payload);
        final Map fromPayloadRaw = underTest.read(raw, Map.class);
        assertEquals(payloadAsMap, fromPayloadRaw);
        testSerializationRoundtrip(payloadAsMap, Map.class);
    }

    private byte[] toBytes(int[] payload) {
        byte[] raw = new byte[payload.length];
        for (int i1 = 0; i1 < payload.length; i1++) {
            raw[i1] = (byte) payload[i1];
        }
        return raw;
    }

}
