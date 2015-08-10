package com.yazino.novomatic.cgs.msgpack;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueType;
import org.msgpack.unpacker.Unpacker;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * MessagePack is not great for generic maps :(
 * See https://github.com/msgpack/msgpack-java/issues/52
 * https://gist.github.com/muga/6569065
 * https://github.com/msgpack/msgpack-java/issues/54
 */
@SuppressWarnings("ALL")
@Component
public class MessagePackMapper {

    private final MessagePack messagePack = new MessagePack();

    public <T> T read(byte[] b, Class<T> clazz, String... binaryKeys) throws IOException {
        Unpacker unpacker = messagePack.createUnpacker(new ByteArrayInputStream(b));
        Value value = unpacker.readValue();
        return (T) deserializeObject(value, null, Arrays.asList(binaryKeys));
    }

    public byte[] write(Object object) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = new MessagePack().createPacker(out);
        packer.write(object);
        packer.close();
        return out.toByteArray();
    }

    private Object deserializeObject(Value value, Object relatedKey, List<String> binaryKeys) {
        ValueType t = value.getType();
        switch (t) {
            case NIL:
                return null;
            case BOOLEAN:
                return value.asBooleanValue().getBoolean();
            case INTEGER:
                return value.asIntegerValue().getLong();
            case FLOAT:
                return value.asFloatValue().getDouble();
            case ARRAY:
                final ArrayValue arrayValues = value.asArrayValue();
                final List arrayResult = new ArrayList(arrayValues.size());
                for (Value arrayValue : arrayValues) {
                    arrayResult.add(deserializeObject(arrayValue, null, binaryKeys));
                }
                return arrayResult;
            case MAP:
                final MapValue mapValue = value.asMapValue();
                final Map<Object, Object> mapResult = new HashMap<Object, Object>();
                for (Map.Entry<Value, Value> e : mapValue.entrySet()) {
                    final Object key = deserializeObject(e.getKey(), null, binaryKeys);
                    final Object val = deserializeObject(e.getValue(), key, binaryKeys);
                    mapResult.put(key, val);
                }
                return mapResult;
            case RAW:  // string
                if (relatedKey != null && binaryKeys.contains(relatedKey)) {
                    return value.asRawValue().getByteArray();
                }
                return value.asRawValue().getString();
            default:
                throw new RuntimeException("Unknown type for value: " + value);
        }
    }
}
