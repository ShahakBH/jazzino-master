package com.yazino.host.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ImmediateHostDocumentPublisher;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.processor.chat.*;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.test.DummyProfanityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class StandaloneChatProcessor implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneChatProcessor.class);

    private final ChatRequestSource requestSource;
    private final Map<ChatRequestType, Object> processors = new HashMap<ChatRequestType, Object>();

    @Autowired
    public StandaloneChatProcessor(final ChatRepository chatRepository,
                                   @Qualifier("standaloneDocumentDispatcher")
                                   final DocumentDispatcher documentDispatcher,
                                   final PlayerRepository playerRepository,
                                   final PlayerSessionRepository playerSessionRepository,
                                   final ChatRequestSource requestSource) {
        this.requestSource = requestSource;
        final ChatChannelWorker chatChannelWorker = new ChatChannelWorker();
        final ProfanityFilter profanityFilter = new ProfanityFilter(new DummyProfanityService());
        final ChatChannelAggregateWorker aggregateWorker = new ChatChannelAggregateWorker(new ImmediateHostDocumentPublisher(documentDispatcher), new DestinationFactory());
        final ChatMessageWorker chatMessageWorker = new ChatMessageWorker(playerRepository,
                playerSessionRepository,
                chatRepository,
                profanityFilter,
                documentDispatcher,
                chatChannelWorker);
        final AddParticipantProcessor addParticipantProcessor = new AddParticipantProcessor(chatRepository, chatChannelWorker);
        final ChatMessageProcessor chatMessageProcessor = new ChatMessageProcessor(chatMessageWorker);
        final LeaveChannelProcessor leaveChannelProcessor = new LeaveChannelProcessor(chatRepository);
        final PublishChannelProcessor publishChannelProcessor = new PublishChannelProcessor(chatRepository, aggregateWorker);

        processors.put(addParticipantProcessor.getTemplate().getRequestType(), addParticipantProcessor);
        processors.put(chatMessageProcessor.getTemplate().getRequestType(), chatMessageProcessor);
        processors.put(leaveChannelProcessor.getTemplate().getRequestType(), leaveChannelProcessor);
        processors.put(publishChannelProcessor.getTemplate().getRequestType(), publishChannelProcessor);
    }

    void process(final GigaspaceChatRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request: " + request);
        }
        processRequestOn(processors.get(request.getRequestType()), request);
    }

    private void processRequestOn(final Object target, final GigaspaceChatRequest request) {
        // This is a nasty hack to get around having to use CGLib proxies in the processors.
        // If we use an interface than any proxy uses the interface, and gigaspace template
        // parsing breaks.

        try {
            final Method processRequest = target.getClass().getMethod("processRequest", GigaspaceChatRequest.class);
            processRequest.invoke(target, request);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            } else {
                LOG.error("Request processing failed", e);
            }
        } catch (Exception e) {
            LOG.error("Request processing failed", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @SuppressWarnings("InfiniteLoopStatement")
            @Override
            public void run() {
                while (true) {
                    final GigaspaceChatRequest request = requestSource.getNextRequest();
                    process(request);
                }
            }
        }).start();
    }
}
