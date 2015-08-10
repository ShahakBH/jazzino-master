package strata.server.worker.event.consumer;


import com.yazino.bi.messaging.CommitAware;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.promotion.PromoRewardEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresPromotionRewardDWDAO;

import java.util.ArrayList;
import java.util.List;

@Component
@Qualifier("promotionRewardEventConsumer")
public class PromotionRewardEventConsumer implements QueueMessageConsumer<PromoRewardEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(PromotionRewardEventConsumer.class);
    private final PostgresPromotionRewardDWDAO promoRewardDWDAO;
    private final ThreadLocal<List<PromoRewardEvent>> batchedMessages = new ThreadLocal<List<PromoRewardEvent>>() {
        @Override
        protected List<PromoRewardEvent> initialValue() {
            return new ArrayList<>();
        }
    };

    @Autowired
    public PromotionRewardEventConsumer(PostgresPromotionRewardDWDAO promoRewardDWDAO) {
        this.promoRewardDWDAO = promoRewardDWDAO;
    }

    @Override
    public void handle(PromoRewardEvent promoRewardEvent) {
        LOG.debug("Received event {}", promoRewardEvent);
        batchedMessages.get().add(promoRewardEvent);
    }

    @Override
    public void consumerCommitting() {
        final List<PromoRewardEvent> messagesForThread = batchedMessages.get();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());
            promoRewardDWDAO.saveAll(new ArrayList<>(messagesForThread));
            messagesForThread.clear();
        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
