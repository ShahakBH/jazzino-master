package strata.server.test.helpers.database;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Helps checking that an SQL string contains no tables from the given static list
 * without an appropriate prefix
 */
public final class PrefixedTablesVerificationHelper {
    private PrefixedTablesVerificationHelper() {
        // No public constructor for utility class
    }

    private static final String[] MAIN_SCHEMA_TABLES = new String[] {
            "ACCOUNT", "ACHIEVEMENT", "AUDIT_CHAT",
            "AUDIT_CLOSED_GAME_OLD", "AUDIT_EVENT", "AUDIT_MESSAGE", "AUDIT_TRANSACTION_RESULT", "AUTHENTICATION",
            "BAD_WORD", "CLIENT", "CLIENT_DUMP", "CLIENT_PROPERTY", "DIAGNOSTIC_REQUEST", "ETL_GVT_MONEY_DAILY",
            "GAME_TYPE", "GAME_VARIATION_TEMPLATE", "GAME_VARIATION_TEMPLATE_PROPERTY", "LEADERBOARD",
            "LEADERBOARD_PLAYER", "LEADERBOARD_POSITION", "LEADERBOARD_RESULT", "LEVEL_SYSTEM", "LOBBY_USER",
            "PARTNER", "PARTNER_USERS", "PLAYER", "PLAYER_INBOX", "PLAYER_RELATIONSHIP", "PLAYER_TROPHY",
            "RECURRING_TOURNAMENT_DEFINITION", "REQUESTMAP", "ROLE", "ROLE_PEOPLE", "SYSTEM_CONFIG", "SYSTEM_MESSAGE",
            "TABLE_GAME_VARIATION", "TABLE_INFO", "TABLE_INVITE", "TABLE_STATUS", "TOPUP_REQUEST", "TOURNAMENT",
            "TOURNAMENT_PLAYER", "TOURNAMENT_SUMMARY", "TOURNAMENT_TABLE", "TOURNAMENT_VARIATION_PAYOUT",
            "TOURNAMENT_VARIATION_ROUND", "TOURNAMENT_VARIATION_TEMPLATE", "TRANSACTION_TYPE", "TROPHY", "USER",
            "VOIDED_GAME", "YAZINO_LOGIN" };

    private static final String[] DW_SCHEMA_TABLES = new String[] {
            "TRANSACTION_LOG", "ACCOUNT_SESSION",
            "AUDIT_CLOSED_GAME", "AUDIT_CLOSED_GAMES_PLAYER", "AUDIT_COMMAND", "EXTERNAL_TRANSACTION", "INVITATIONS",
            "ACCOUNT", "LOBBY_USER", "PLAYER", "PLAYER_LEVEL", "PLAYER_DEFINITION", "GAME_VARIATION_TEMPLATE",
            "TABLE_DEFINITION", "LEADERBOARD", "LEADERBOARD_POSITION", "TOURNAMENT", "TOURNAMENT_SUMMARY",
            "TOURNAMENT_PLAYER", "TOURNAMENT_PLAYER_SUMMARY", "TOURNAMENT_VARIATION_TEMPLATE", "YAZINO_LOGIN",
            "GAME_TYPE", "TABLE_INFO" };

    private static final String MAIN_SCHEMA_NAME = "strataprod";
    private static final String DW_SCHEMA_NAME = "strataproddw";

    /**
     * Checks that the sql query contains no tables that are not prefixed, make the hosting test fail if needed
     * @param sql SQL string to verify
     */
    public static void assertNoNonPrefixedTables(final String sql) {
        for (final String tableName : MAIN_SCHEMA_TABLES) {
            // TODO: replace by a regular expression
            // "contains table name with two whitespace characters around"
            if (sql.contains(" " + tableName + " ") || sql.contains("\t" + tableName + " ")
                    || sql.contains("\n" + tableName + " ") || sql.contains(" " + tableName + "\t")
                    || sql.contains("\t" + tableName + "\t") || sql.contains("\n" + tableName + "\n")
                    || sql.contains(" " + tableName + "\n") || sql.contains("\t" + tableName + "\n")
                    || sql.contains("\n" + tableName + "\n")) {
                fail(getFailureMessage("The query contains a non-prefixed table name: ", sql, " " + tableName));
            }
            if (sql.contains(DW_SCHEMA_NAME + "." + tableName)) {
                // this might be a table duplicated in DW schema...
                if (!Arrays.asList(DW_SCHEMA_TABLES).contains(tableName)) {
                    fail(getFailureMessage("The query contains the misplaced table from the main schema: ", sql,
                            DW_SCHEMA_NAME + "." + tableName));
                }
            }
        }
        for (final String tableName : DW_SCHEMA_TABLES) {
            if (sql.contains(MAIN_SCHEMA_NAME + "." + tableName)) {
                // this might be a table duplicated from prod schema...
                if (!Arrays.asList(MAIN_SCHEMA_TABLES).contains(tableName)) {
                    fail(getFailureMessage("The query contains the misplaced table from the DW schema: ", sql,
                            MAIN_SCHEMA_NAME + "." + tableName));
                }
            }
        }
    }

    /**
     * Builds the failure message
     * @param errorMessage Error description
     * @param sql SQL string containing the wrong table name
     * @param tableName Table name in error
     * @return String to put to the failure call
     */
    private static String getFailureMessage(final String errorMessage, final String sql, final String tableName) {
        final int index = sql.indexOf(tableName);
        final StringBuilder failureMessage = new StringBuilder(errorMessage);
        failureMessage.append(sql.substring(0, index)).append(" ------->>> ").append(tableName).append(" <<<------- ")
                .append(sql.substring(index + tableName.length() + 1));
        return failureMessage.toString();
    }
}
