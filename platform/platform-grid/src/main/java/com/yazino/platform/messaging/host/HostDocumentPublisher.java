package com.yazino.platform.messaging.host;


import java.util.Collection;

public interface HostDocumentPublisher {

    void publish(Collection<HostDocument> hostDocuments);

}
