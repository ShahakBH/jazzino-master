package strata.server.lobby.api.promotion;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class PromotionConfigurationTest {
    private static final String KNOWN_STRING_KEY = "string.key";
    private static final String KNOWN_STRING_VALUE = "string.value";
    private static final String KNOWN_INT_KEY = "int.key";
    private static final String KNOWN_INT_VALUE = "678";
    private PromotionConfiguration underTest;

    @Before
    public void init() {
        underTest = new PromotionConfiguration();
        underTest.addConfigurationItem(KNOWN_STRING_KEY, KNOWN_STRING_VALUE);
        underTest.addConfigurationItem(KNOWN_INT_KEY, KNOWN_INT_VALUE);
    }

    @Test
    public void shouldReturnValueAsString() throws Exception {
        assertThat(underTest.getConfigurationValue(KNOWN_STRING_KEY), is(KNOWN_STRING_VALUE));
    }

    @Test
    public void shouldReturnNullForUnknownKey() throws Exception {
        assertNull(underTest.getConfigurationValue("unknown key"));
    }

    @Test
    public void shouldReturnValueAsInteger() throws Exception {
        assertThat(underTest.getConfigurationValueAsInteger(KNOWN_INT_KEY), is(Integer.parseInt(KNOWN_INT_VALUE)));
    }

    @Test
    public void cannotAddNullKey() throws Exception {
        underTest = new PromotionConfiguration();
        underTest.addConfigurationItem(null, "value");
        assertTrue(underTest.getConfiguration().isEmpty());
    }

    @Test
    public void cannotAddNullValue() throws Exception {
        underTest = new PromotionConfiguration();
        underTest.addConfigurationItem("key", null);
        assertTrue(underTest.getConfiguration().isEmpty());
    }
}
