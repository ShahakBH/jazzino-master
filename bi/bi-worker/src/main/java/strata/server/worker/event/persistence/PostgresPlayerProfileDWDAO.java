package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.player.PlayerProfileStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresPlayerProfileDWDAO extends PostgresDWDAO<PlayerProfileEvent> {
    private static final String SQL_EXECUTE_UPDATES = "UPDATE LOBBY_USER SET "
            + "PLAYER_ID = stage.PLAYER_ID, DISPLAY_NAME = stage.DISPLAY_NAME,"//yes reg_ts is not here. that is correct
            + " REAL_NAME = stage.REAL_NAME, FIRST_NAME = stage.FIRST_NAME, PICTURE_LOCATION = stage.PICTURE_LOCATION,"
            + " EMAIL_ADDRESS = stage.EMAIL_ADDRESS, COUNTRY = stage.COUNTRY, EXTERNAL_ID = stage.EXTERNAL_ID,"
            + " VERIFICATION_IDENTIFIER = stage.VERIFICATION_IDENTIFIER, PROVIDER_NAME = stage.PROVIDER_NAME,"
            + " RPX_PROVIDER = stage.RPX_PROVIDER, STATUS = stage.STATUS, DATE_OF_BIRTH = stage.DATE_OF_BIRTH,"
            + " GENDER = stage.GENDER, REFERRAL_ID = stage.REFERRAL_ID, GUEST_STATUS = stage.GUEST_STATUS,"
            + " PARTNER_ID = stage.PARTNER_ID "
            + "FROM STG_LOBBY_USER stage "
            + "WHERE LOBBY_USER.PLAYER_ID = stage.PLAYER_ID";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO LOBBY_USER "
            + "SELECT stage.* FROM STG_LOBBY_USER stage "
            + "LEFT JOIN LOBBY_USER target ON "
            + "stage.PLAYER_ID = target.PLAYER_ID "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_LOBBY_USER";

    public PostgresPlayerProfileDWDAO() {
        super(null);
    }

    @Autowired
    public PostgresPlayerProfileDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<PlayerProfileEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }

    private String createInsertStatementFor(final List<PlayerProfileEvent> playerProfileEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_LOBBY_USER", "PLAYER_ID",
                "REG_TS", "DISPLAY_NAME", "REAL_NAME", "FIRST_NAME", "PICTURE_LOCATION", "EMAIL_ADDRESS",
                "COUNTRY", "EXTERNAL_ID", "VERIFICATION_IDENTIFIER", "PROVIDER_NAME", "RPX_PROVIDER", "STATUS",
                "DATE_OF_BIRTH", "GENDER", "REFERRAL_ID", "GUEST_STATUS", "PARTNER_ID");
        for (PlayerProfileEvent playerProfileEvent : playerProfileEvents) {

            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(playerProfileEvent.getPlayerId()),
                    sqlTimestamp(playerProfileEvent.getRegistrationTime()),
                    sqlString(playerProfileEvent.getDisplayName()),
                    sqlString(playerProfileEvent.getRealName()),
                    sqlString(playerProfileEvent.getFirstName()),
                    sqlString(playerProfileEvent.getPictureLocation()),
                    sqlString(playerProfileEvent.getEmail()),
                    sqlString(playerProfileEvent.getCountry()),
                    sqlString(playerProfileEvent.getExternalId()),
                    sqlString(playerProfileEvent.getVerificationIdentifier()),
                    sqlString(playerProfileEvent.getProviderName()),
                    sqlString(playerProfileEvent.getProviderName()),
                    sqlString(idOrDefaultValueFor(playerProfileEvent.getStatus())),
                    sqlTimestamp(playerProfileEvent.getDateOfBirth()),
                    sqlString(truncateGender(playerProfileEvent.getGender())),
                    sqlString(playerProfileEvent.getInviteReferrerId()),
                    sqlString(truncateGuestStatus(playerProfileEvent.getGuestStatus())),
                    sqlString(playerProfileEvent.getPartnerId().name())
            );
        }

        return insertBuilder.toSql();
    }

    private String idOrDefaultValueFor(final PlayerProfileStatus status) {
        if (status == null) {
            return PlayerProfileStatus.ACTIVE.getId();
        }
        return status.getId();
    }

    private String truncateGender(String gender) {
        return firstLetterOf(gender);
    }

    private String truncateGuestStatus(String guestStatus) {
        return firstLetterOf(guestStatus);
    }

    private String firstLetterOf(String gender) {
        if (gender != null && gender.length() > 0) {
            return gender.substring(0, 1);
        } else {
            return null;
        }
    }

}
