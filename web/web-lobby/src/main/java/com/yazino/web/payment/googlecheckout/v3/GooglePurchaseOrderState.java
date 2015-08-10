package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 * See http://developer.android.com/google/play/billing/billing_reference.html
 */
@JsonSerialize(using = GooglePurchaseOrderState.MySerializer.class)
public enum GooglePurchaseOrderState {
    PURCHASED(0),
    CANCELED(1),
    REFUNDED(2);

    private int code;

    GooglePurchaseOrderState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static class MySerializer extends JsonSerializer<GooglePurchaseOrderState> {
        public MySerializer() {
        }

        @Override
        public void serialize(final GooglePurchaseOrderState value,
                              final JsonGenerator generator,
                              final SerializerProvider provider)
                throws IOException {
            generator.writeNumber(value.getCode());
        }
    }
}
