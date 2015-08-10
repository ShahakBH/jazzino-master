package com.yazino.platform.lightstreamer.adapter;

import com.lightstreamer.interfaces.data.ItemEventListener;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.lightstreamer.adapter.CompressionUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MobileMessageListenerAdapterTest {

    private final String mSubject = "aSubject";
    private final ItemEventListener mEventListener = mock(ItemEventListener.class);
    private final MobileMessageListenerAdapter mAdapter = new MobileMessageListenerAdapter(mSubject, mEventListener);

    @Test
    public void shouldCallListenerWithMapContainingBodyAndContentTypeWhenUncompressed() throws Exception {
        Message message = toMessage("aBody", "aContentType", false);
        mAdapter.onMessage(message);
        verify(mEventListener).update(mSubject, toMap("aContentType", "aBody"), false);
    }

    @Test
    public void shouldCallListenerWithMapContainingBodyWhenCompressed() throws Exception {
        Message message = toMessage("aBody", "aContentType", true);
        mAdapter.onMessage(message);
        verify(mEventListener).update(mSubject, toMap("aContentType", "aBody"), false);
    }

    private static Message toMessage(String body, String contentType, boolean compressed) throws IOException {
        String messageBody = body;
        MessageProperties properties = new MessageProperties();
        properties.setContentType(contentType);
        if (compressed) {
            messageBody = toBase64(deflate(body));
            properties.setContentEncoding("DEF");
        }
        return new Message(messageBody.getBytes(UTF_8), properties);
    }

    private static Map<String, String> toMap(String contentType, String body) {
        Map<String, String> map = new HashMap<String, String>(2);
        map.put("contentType", contentType);
        map.put("body", body);
        return map;
    }


}
