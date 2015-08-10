package com.yazino.platform.bonus;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.bonus.persistence.BonusDao;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.BonusCollectedEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;
import static org.joda.time.DateTime.now;

@Service("bonusService")
public class RemotingBonusService implements BonusService {

    public static final int DEFAULT_LOCKOUT = 4 * 60;
    public static final long DEFAULT_BONUS = 2000L;
    public static final long A_MINUTE = 60 * 1000L;

    private final BonusDao bonusDao;
    private final YazinoConfiguration configuration;
    private final PlayerService playerService;

    protected static final String AMOUNT = "bonus.amount";
    protected static final String LOCKOUT = "bonus.lockout.in.minutes";

    private static final Logger LOG = LoggerFactory.getLogger(RemotingBonusService.class);
    private QueuePublishingService<BonusCollectedEvent> bonusEventService;

    @Autowired
    public RemotingBonusService(final BonusDao bonusDao,
                                final PlayerService playerService,
                                final YazinoConfiguration configuration,
                                @Qualifier("bonusCollectedEventQueuePublishingService") final QueuePublishingService<BonusCollectedEvent> bonusEventService) {
        notNull(bonusDao);
        notNull(playerService);
        notNull(configuration);
        notNull(bonusEventService);
        this.bonusDao = bonusDao;
        this.playerService = playerService;
        this.configuration = configuration;
        this.bonusEventService = bonusEventService;
    }

    @Override
    public BonusStatus getBonusStatus(final BigDecimal playerId) {
        final DateTime lastLockoutTime = bonusDao.getLastBonusTime(playerId);
        final long bonusAmount = configuration.getLong(AMOUNT, DEFAULT_BONUS);
        final int lockoutInMinutes = configuration.getInt(LOCKOUT, DEFAULT_LOCKOUT);
        final long lockoutLengthInMillis = lockoutInMinutes * A_MINUTE;

        if (lastLockoutTime == null) {
            return new BonusStatus(0L, bonusAmount);
        }
        return new BonusStatus(lastLockoutTime.getMillis() + lockoutLengthInMillis - now().getMillis(), bonusAmount);
    }

    @Override
    public BonusStatus collectBonus(final BigDecimal playerId, final BigDecimal sessionId) throws BonusException {
        final Long lockoutExpiry = getBonusStatus(playerId).getMilliesToNextBonus();
        if (lockoutExpiry > 0) {
            throw new BonusException("Lockout has not expired:" + lockoutExpiry);
        }
        DateTime now = now();

        final int lockoutInMins = configuration.getInt(LOCKOUT, DEFAULT_LOCKOUT);
        final long amount = configuration.getLong(AMOUNT, DEFAULT_BONUS);

        final Long nextLockoutTime = (lockoutInMins * A_MINUTE);

        bonusDao.setLockoutTime(playerId, now);

        try {
            playerService.postTransaction(playerId,
                    sessionId,
                    BigDecimal.valueOf(amount),
                    "LockoutBonus", "LockoutBonus " + amount);
            bonusEventService.send(new BonusCollectedEvent(playerId, now));

            return new BonusStatus(nextLockoutTime, amount);

        } catch (WalletServiceException e) {
            LOG.error(String.format("error trying to give player %s with %d chips for Bonus ", playerId, amount), e);
            throw new BonusException(String.format("Internal Wallet error: error trying to give player %s with %d chips for Bonus ", playerId, amount), e);
        }

    }
}
