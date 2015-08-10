package utils;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.ReadableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static org.joda.time.DateTime.now;

public class PlayerBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerBuilder.class);
    Map<String, Object> params;
    public static final int ANDY = 1;
    public static final int BOB = 2;
    public static final int CHAZ = 3;
    public static final int DAVE = 4;
    public static final int ERNIE = 5;
    public static final int FRANK = 6;
    public static final int GEORGE = 7;
    public static final int HENRY = 8;
    public static final int IAIN = 9;

    public static DateTime today = new DateTime().withTimeAtStartOfDay();

    public static DateTime yesterday = new DateTime().withTimeAtStartOfDay().minusDays(1);

    public static DateTime dayBeforeYesterday = new DateTime().withTimeAtStartOfDay().minusDays(2);

    public static DateTime aBitAgo = new DateTime().withTimeAtStartOfDay().minusDays(4);

    public static DateTime lastWeek = new DateTime().withTimeAtStartOfDay().minusDays(7);
    public static DateTime lastMonth = new DateTime().withTimeAtStartOfDay().minusMonths(1);
    public static DateTime lastYear = new DateTime().withTimeAtStartOfDay().minusYears(1);


    public PlayerBuilder(final boolean create) {
        params = Maps.newHashMap();
        params.put("purchases", new HashSet<Purchase>());
        params.put("played", new HashSet<Timestamp>());
        params.put("reg_platform", "IOS");
        params.put("game_type", "SLOTS");
        params.put("referrer", null);
        params.put("create", create);
        params.put("balance", BigDecimal.valueOf(100));

    }

    public PlayerBuilder aGameOf(String gameType) {
        params.put("game_type", gameType);
        return this;
    }

    public PlayerBuilder withName(final String name) {
        params.put("name", name);
        return this;
    }

    public PlayerBuilder withId(final Integer playerId) {
        params.put("player_id", playerId);
        return this;
    }

    public PlayerBuilder withAnIphone() {
        params.put("iPhone", "");
        return this;
    }

    public PlayerBuilder withAnAndroid() {
        params.put("android", "");
        return this;
    }


    public PlayerBuilder whoRegistered(final ReadableDateTime dateRegistered) {
        params.put("registered", toTS(dateRegistered));

        return whoLoggedIn(dateRegistered);//can't reg without an account_session
    }

    public PlayerBuilder withReferrer(final String ref) {
        params.put("referrer", ref);
        return this;
    }

    public PlayerBuilder whoPlayed(final ReadableDateTime... datePlayed) {
        for (ReadableDateTime dateMidnight : datePlayed) {
            ((Set<Timestamp>) params.get("played")).add(toTS(dateMidnight));
        }
        return this;
    }

    public PlayerBuilder whoBoughtChips(Purchase... purchases) {
        for (Purchase purchase : purchases) {
            ((Set<Purchase>) params.get("purchases")).add(purchase);
        }
        return this;
    }

    public PlayerBuilder whoLoggedIn(final ReadableDateTime... times) {

        params.put("loggedIn", times);

        return this;
    }

    public PlayerBuilder withCampaignRunSegmentFor(final long campaignRunId) {
        params.put("campaignRunId", campaignRunId);
        return this;
    }

    public PlayerBuilder withLevel(final int level) {
        params.put("level", level);
        return this;
    }

    public PlayerBuilder withOtherLevel(final int level) {
        params.put("otherlevel", level);
        return this;
    }

    public PlayerBuilder withSomeBets(final int i) {
        params.put("bets", i);
        return this;
    }

    public PlayerBuilder withBonusCollections(final DateTime... collectionTimes) {
        params.put("collections", collectionTimes);
        return this;
    }

    private PlayerBuilder withAccountId(final int accountId) {
        params.put("account_id", accountId);
        return this;
    }

    public PlayerBuilder withBalance(final long amount) {
        params.put("balance", amount);
        return this;
    }

    int internalTrannyId = 0;

    public void storeIn(final NamedParameterJdbcTemplate template) {
        LOG.debug("Creating player with params:{}", params);
        if (params.get("registered") == null) {
            params.put("registered", toTS(new DateTime()));
        }
        if ((Boolean) params.get("create")) {
            template.update(
                    "insert into lobby_user (player_id, provider_name, rpx_provider, display_name, account_id, reg_ts, registration_platform, registration_game_type, registration_referrer) " +
                            "values (:player_id, 'provider','provider', :name, :account_id, :registered, :reg_platform, :game_type, :referrer)",
                    params);


            template.update("insert into account values (:account_id, :balance)", params);
        }
        if (params.get("loggedIn") != null) {
            ReadableDateTime[] times = (ReadableDateTime[]) params.get("loggedIn");
            for (ReadableDateTime time : times) {
                params.put("loggedInTime", toTS(time));
                template.update(
                        "insert into account_session(account_id, session_key,start_ts) values (:account_id,'KEY',:loggedInTime)",
                        params);
            }

        }

        if (params.get("played") != null) {
            final Set<Timestamp> played = (Set<Timestamp>) params.get("played");
            for (Timestamp play : played) {
                params.put("play", play);//bit hacky this.
                template.update(
                        "insert into player_activity_daily values(:player_id,:game_type,:reg_platform,:play,:referrer,:registered)", params);
                template.update(
                        "insert into player_activity_hourly values(:player_id,:game_type,:reg_platform,:play,:play)", params);
            }
        }

        if (params.get("collections") != null) {
            ReadableDateTime[] times = (ReadableDateTime[]) params.get("collections");
            for (ReadableDateTime time : times) {
                params.put("tranny_ts", toTS(time));
                template.update(
                        "INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                                "VALUES(:account_id,1000,'LockoutBonus',:tranny_ts, 'test_tranny', -777.01, 0.00 )",
                        params);

            }

        }

        if (params.containsKey("iPhone")) {
            template.update("insert into mobile_device (player_id, game_type,platform,app_id,device_id,push_token,active) " +
                    "values(:player_id, 'SLOTS', 'IOS', 'com.yazino.YazinoApp', :player_id, 'test-token', true)", params);
        }

        if (params.containsKey("android")) {
            template.update("insert into mobile_device (player_id, game_type,platform,app_id,device_id,push_token,active) " +
                    "values(:player_id, 'SLOTS', 'ANDROID', 'com.yazino.YazinoApp', :player_id, 'test-token', true)", params);
        }
        if (params.containsKey("campaignRunId")) {
            template.update("insert into segment_selection (campaign_run_id, player_id,valid_from) values (:campaignRunId,:player_id,null)", params);

        }
        if (params.containsKey("level")) {
            template.update("insert into player_level values(:player_id, 'SLOTS', :level)", params);
        }

        if (params.containsKey("otherlevel")) {
            template.update("insert into player_level values(:player_id, 'BLACKJACK', :otherlevel)", params);
        }

        if (params.containsKey("bets")) {
            for (int j = 1; j < (int) params.get("bets") + 1; j++) {
                params.put("time", toTS(now().minusMinutes(j * 10)));
                template.update("INSERT INTO TRANSACTION_LOG(ACCOUNT_ID,AMOUNT,TRANSACTION_TYPE,TRANSACTION_TS,REFERENCE, TABLE_ID, GAME_ID) " +
                        "VALUES(:account_id,1000,'Stake',:time, 'test_tranny', -777.01, 0.00 )", params);
            }
        }

        final Set<Purchase> purchases = (Set<Purchase>) params.get("purchases");

        if (purchases.size() > 0) {
            for (Purchase purchase : purchases) {
                params.put("purchase", purchase);//bit hacky this.
                params.put("internal_tranny_id", (Integer) params.get("player_id") + "666" + internalTrannyId++);
                params.put("message_ts", toTS(purchase.dateBought));
                params.put("amount", purchase.amount);
                params.put("currency_code", purchase.currency);
                params.put("chips", purchase.amount * 100);
                params.put("status", purchase.status);
                template.update("insert into external_transaction (" +
                                "account_id, " +
                                "internal_transaction_id, " +
                                "message, " +
                                "message_ts, " +
                                "currency_code," +
                                "amount, " +
                                "credit_card_number, " +
                                "cashier_name, " +
                                "external_transaction_status, " +
                                "amount_chips, " +
                                "version, " +
                                "platform, " +
                                "player_id" +
                                ") values (" +
                                ":account_id, " +
                                ":internal_tranny_id, " +
                                "'message', " +
                                ":message_ts, " +
                                ":currency_code, " +
                                ":amount, " +
                                "''," +
                                "'iTunes'," +
                                ":status," +
                                ":chips, " +
                                "1," +
                                ":reg_platform," +
                                ":player_id" +
                                ")",
                        params);
            }
        }

    }

    public static Timestamp toTS(final ReadableDateTime dateTime) {
        return new Timestamp(dateTime.getMillis());
    }


    public static final String[] names = {"padding", "Andy", "Bob", "Chaz", "Dave", "Ernie", "Frank", "George", "Henry", "Iain"};

    public static PlayerBuilder createPlayer(final int playerId) {
        return new PlayerBuilder(true).withName(names[playerId]).withId(playerId).withAccountId(10 + playerId);
    }

    public static PlayerBuilder getPlayer(final int playerId) {
        return new PlayerBuilder(false).withName(names[playerId]).withId(playerId).withAccountId(10 + playerId);
    }

    public static void initialise(final NamedParameterJdbcTemplate externalDwNamedJdbcTemplate) {
        //for if we're using ThreadLocalDateTimeUtils...
        today = new DateTime().withTimeAtStartOfDay();
        yesterday = new DateTime().withTimeAtStartOfDay().minusDays(1);
        dayBeforeYesterday = new DateTime().withTimeAtStartOfDay().minusDays(2);
        aBitAgo = new DateTime().withTimeAtStartOfDay().minusDays(4);
        lastWeek = new DateTime().withTimeAtStartOfDay().minusDays(7);
        lastMonth = new DateTime().withTimeAtStartOfDay().minusMonths(1);
        lastYear = new DateTime().withTimeAtStartOfDay().minusYears(1);


        Map<String, Object> params = newHashMap();
        externalDwNamedJdbcTemplate.update("delete from lobby_user", params);
        externalDwNamedJdbcTemplate.update("delete from account", params);
        externalDwNamedJdbcTemplate.update("delete from account_session", params);
        externalDwNamedJdbcTemplate.update("delete from player_activity_daily", params);
        externalDwNamedJdbcTemplate.update("delete from player_activity_hourly", params);
        externalDwNamedJdbcTemplate.update("delete from external_transaction", params);
        externalDwNamedJdbcTemplate.update("delete from mobile_device", params);
        externalDwNamedJdbcTemplate.update("delete from player_level", params);
        externalDwNamedJdbcTemplate.update("delete from audit_command where audit_label='test_bet'", params);
        externalDwNamedJdbcTemplate.update("delete from transaction_log where reference='test_tranny'", params);
    }


    public static class Purchase {

        public final ReadableDateTime dateBought;
        public final int amount;
        public String currency = "GBP";
        public String status = "SUCCESS";


        public Purchase(final ReadableDateTime dateBought, final int amountOfCash) {
            this.dateBought = dateBought;
            this.amount = amountOfCash;
        }

        public Purchase withCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Purchase withStatus(String status) {
            this.status = status;
            return this;
        }

    }

}
