package com.yazino.platform.gamehost.external;

import java.util.Collection;

public interface ExternalGameRequestService {

    void postRequests(Collection<NovomaticRequest> requests);
}
