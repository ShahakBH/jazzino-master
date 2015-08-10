package strata.server.worker.audit.playedtracking.repository;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import com.yazino.platform.event.message.PlayerPlayedEvent;

import java.math.BigDecimal;

import static org.springframework.util.Assert.notNull;

@Repository("playerPlayedEventRepository")
public class EhCachePlayerPlayedEventRepository implements PlayerPlayedEventRepository {
    private final Ehcache cache;

    @Autowired
    public EhCachePlayerPlayedEventRepository(@Qualifier("playerPlayedEventCache") final Ehcache cache) {
        notNull(cache, "cache is null");
        this.cache = cache;
    }

    @Override
    public PlayerPlayedEvent forAccount(final BigDecimal accountId) {
        notNull(accountId, "accountId is null");
        final Element element = cache.get(accountId);
        if (element == null) {
            return null;
        }
        return (PlayerPlayedEvent) element.getValue();
    }

    @Override
    public void store(final BigDecimal accountId, final PlayerPlayedEvent event) {
        notNull(accountId, "accountId is null");
        notNull(event, "event is null");
        cache.put(new Element(accountId, event));
    }
}
