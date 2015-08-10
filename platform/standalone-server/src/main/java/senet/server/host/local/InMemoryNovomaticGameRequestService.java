package senet.server.host.local;

import com.yazino.platform.gamehost.external.ExternalGameRequestService;
import com.yazino.platform.gamehost.external.NovomaticRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InMemoryNovomaticGameRequestService implements ExternalGameRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryNovomaticGameRequestService.class);
    private Collection<NovomaticRequest> pendingRequests = new ArrayList<>();

    @Override
    public void postRequests(Collection<NovomaticRequest> requests) {
        LOG.debug("Storing pending requests {}", requests);
        pendingRequests.addAll(requests);
    }

    public Collection<NovomaticRequest> getAndClearPendingRequests() {
        final List<NovomaticRequest> result = new ArrayList<>(pendingRequests);
        pendingRequests.clear();
        return result;
    }
}
