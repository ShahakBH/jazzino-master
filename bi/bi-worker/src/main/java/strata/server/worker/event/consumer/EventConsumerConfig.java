package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.BIWorkerServiceFactory;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.EventWorkerServersConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import strata.server.worker.event.consumer.crm.PlayerVerifiedMessageConsumer;
import strata.server.worker.tracking.TrackingEventConsumer;

@Configuration
@Import(EventWorkerServersConfiguration.class)
public class EventConsumerConfig {
    private BIWorkerServiceFactory factory = new BIWorkerServiceFactory();

    @Autowired(required = true)
    @Qualifier("eventWorkerServers")
    private WorkerServers workerServers;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Value("${strata.rabbitmq.event.exchange}")
    private String exchangeName;

    @Bean
    public SimpleMessageListenerContainer accountEventConsumerContainer(
            @Value("${strata.rabbitmq.event.account.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.account.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.account.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.account.batch-size}") final int batchSize,
            @Qualifier("accountEventConsumer") final AccountEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer leaderboardEventConsumerContainer(
            @Value("${strata.rabbitmq.event.leaderboard.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.leaderboard.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.leaderboard.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.leaderboard.batch-size}") final int batchSize,
            @Qualifier("leaderboardEventConsumer") final LeaderboardEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer playerEventConsumerContainer(
            @Value("${strata.rabbitmq.event.player.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.player.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.player.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.player.batch-size}") final int batchSize,
            @Qualifier("playerEventConsumer") final PlayerEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer playerReferrerEventConsumerContainer(
            @Value("${strata.rabbitmq.event.playerreferrer.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.playerreferrer.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.playerreferrer.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.playerreferrer.batch-size}") final int batchSize,
            @Qualifier("playerReferrerEventConsumer") final PlayerReferrerEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer playerLevelProfileEventConsumerContainer(
            @Value("${strata.rabbitmq.event.playerlevel.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.playerlevel.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.playerlevel.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.playerlevel.batch-size}") final int batchSize,
            @Qualifier("playerLevelEventConsumer") final PlayerLevelEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer playerProfileEventConsumerContainer(
            @Value("${strata.rabbitmq.event.playerprofile.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.playerprofile.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.playerprofile.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.playerprofile.batch-size}") final int batchSize,
            @Qualifier("playerProfileEventConsumer") final PlayerProfileEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer tableEventConsumerContainer(
            @Value("${strata.rabbitmq.event.table.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.table.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.table.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.table.batch-size}") final int batchSize,
            @Qualifier("tableEventConsumer") final TableEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer promotionRewardEventConsumerContainer(
            @Value("${strata.rabbitmq.event.promotionreward.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.promotionreward.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.promotionreward.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.promotionreward.batch-size}") final int batchSize,
            @Qualifier("promotionRewardEventConsumer") final PromotionRewardEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer tournamentSummaryEventConsumerContainer(
            @Value("${strata.rabbitmq.event.tournamentsummary.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.tournamentsummary.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.tournamentsummary.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.tournamentsummary.batch-size}") final int batchSize,
            @Qualifier("tournamentSummaryEventConsumer") final TournamentSummaryEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer invitationEventConsumerContainer(
            @Value("${strata.rabbitmq.event.invitation.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.invitation.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.invitation.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.invitation.batch-size}") final int batchSize,
            @Qualifier("invitationEventConsumer") final InvitationEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startPlayerVerifiedMessageConsumer(
            @Value("${strata.rabbitmq.platform.player-verified.queue}") final String queueName,
            @Value("${strata.rabbitmq.platform.player-verified.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.platform.player-verified.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.platform.player-verified.batch-size}") final int batchSize,
            final PlayerVerifiedMessageConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startGoogleCloudMessagingDeviceRegistrationEventConsumer(
            @Value("${strata.rabbitmq.platform.messaging-device-registration.queue}") final String queueName,
            @Value("${strata.rabbitmq.platform.messaging-device-registration.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.platform.google-cloud-messaging-device-registration.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.platform.google-cloud-messaging-device-registration.batch-size}") final int batchSize,
            @Qualifier("messagingDeviceRegistrationEventConsumer") final MessagingDeviceRegistrationEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startTrackingEventConsumer(
            @Value("${strata.rabbitmq.tracking.queue}") final String queueName,
            @Value("${strata.rabbitmq.tracking.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.tracking.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.tracking.batch-size}") final int batchSize,
            @Qualifier("trackingEventConsumer") final TrackingEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startClientLogEventConsumer(
            @Value("${strata.rabbitmq.event.client-log.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.client-log.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.client-log.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.client-log.batch-size}") final int batchSize,
            @Qualifier("clientLogEventConsumer") final ClientLogEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startEmailValidationEventConsumer(
            @Value("${strata.rabbitmq.event.emailvalidation.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.emailvalidation.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.emailvalidation.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.emailvalidation.batch-size}") final int batchSize,
            @Qualifier("emailValidationEventConsumer") final EmailValidationEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startGiftSentEventConsumer(
            @Value("${strata.rabbitmq.event.giftsent.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.giftsent.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.giftsent.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.giftsent.batch-size}") final int batchSize,
            @Qualifier("giftSentEventConsumer") final GiftSentEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startGiftCollectedEventConsumer(
            @Value("${strata.rabbitmq.event.giftcollected.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.giftcollected.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.giftcollected.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.giftcollected.batch-size}") final int batchSize,
            @Qualifier("giftCollectedEventConsumer") final GiftCollectedEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startBonusCollectedEventConsumer(
            @Value("${strata.rabbitmq.event.bonuscollected.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.bonuscollected.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.event.bonuscollected.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.event.bonuscollected.batch-size}") final int batchSize,
            @Qualifier("bonusCollectedEventConsumer") final BonusCollectedEventConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName,
                queueName, routingKey, consumer, consumerCount, batchSize);
    }

}
