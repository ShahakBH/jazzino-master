package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentDispatcher;

import java.io.Serializable;

/**
 * A document to send to the clients.
 * <p/>
 * If you'd like a layer of abstraction for testing, see {@link HostDocumentDispatcher}.
 */
public interface HostDocument extends Serializable {

    void send(DocumentDispatcher documentDispatcher);

}
