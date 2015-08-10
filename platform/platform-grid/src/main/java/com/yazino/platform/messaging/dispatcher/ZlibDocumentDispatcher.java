package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class ZlibDocumentDispatcher implements DocumentDispatcher {

    private static final String ZLIB_ENCODING = "DEF";

    private final DocumentDispatcher delegate;

    public ZlibDocumentDispatcher(final DocumentDispatcher delegate) {
        notNull(delegate, "Delegate may not be null");

        this.delegate = delegate;
    }

    @Override
    public void dispatch(final Document document) {
        delegate.dispatch(compress(document));
    }

    @Override
    public void dispatch(final Document document, final BigDecimal playerId) {
        delegate.dispatch(compress(document), playerId);
    }

    @Override
    public void dispatch(final Document document, final Set<BigDecimal> playerIds) {
        delegate.dispatch(compress(document), playerIds);
    }

    private Document compress(final Document document) {
        if (document == null) {
            return null;
        }

        if (StringUtils.isBlank(document.getBody())) {
            return document;
        }

        try {
            return new Document(document.getType(),
                    ZLib.deflate(document.getBody()), document.getHeaders(), ZLIB_ENCODING);

        } catch (IOException e) {
            throw new RuntimeException("Document compression failed", e);
        }
    }
}
