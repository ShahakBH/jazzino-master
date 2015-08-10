package strata.server.test.helpers.database;

import org.junit.Test;

import static org.junit.Assert.fail;

public class PrefixedTablesVerificationHelperTest {

    @Test
    public void shouldNotAllowSQLWithNonPrefixedTableReferences() {

        final String sql = "SELECT PLAYER_ID FROM PLAYER WHERE PLAYER_ID = 12345";

        try {
            PrefixedTablesVerificationHelper.assertNoNonPrefixedTables(sql);
            fail("Did not detect non-prefixed table PLAYER");
        } catch (AssertionError e) {
            // success
        }
    }

    @Test
    public void shouldPreventReferencesToNonExistantProdTables() {

        final String sql = "SELECT blah FROM strataprod.INVITATIONS WHERE something = somethingElse";

        try {
            PrefixedTablesVerificationHelper.assertNoNonPrefixedTables(sql);
            fail("Did not detect incorrect reference to strataprod.INVITATIONS");
        } catch (AssertionError e) {
            // success
        }
    }

    @Test
    public void shouldPreventReferencesToNonExistantDWTables() {

        final String sql = "SELECT blah FROM strataproddw.AUTHENTICATION WHERE something = somethingElse";

        try {
            PrefixedTablesVerificationHelper.assertNoNonPrefixedTables(sql);
            fail("Did not detect incorrect reference to strataproddw.AUTHENTICATION");
        } catch (AssertionError e) {
            // success
        }
    }

    @Test
    public void shouldAllowReferencesToTablesDuplicatedInDW() {
        
        final String sql = "SELECT PLAYER_ID FROM strataproddw.PLAYER WHERE PLAYER_ID = 12345";

        PrefixedTablesVerificationHelper.assertNoNonPrefixedTables(sql);
    }

    @Test
    public void shouldAllowReferencesToProdTablesDuplicatedInDW() {
        
        final String sql = "SELECT PLAYER_ID FROM strataprod.PLAYER WHERE PLAYER_ID = 12345";

        PrefixedTablesVerificationHelper.assertNoNonPrefixedTables(sql);
    }

}
