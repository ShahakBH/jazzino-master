package strata.server.lobby.api.facebook;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FacebookHttpClientFactoryBeanTest {

    FacebookHttpClientFactoryBean underTest;

    @Before
    public void initUnderTest() throws Exception {
        underTest = new FacebookHttpClientFactoryBean();
        underTest.afterPropertiesSet();
    }

    @Test
    public void shouldReturnObjectTypeOfHttpClient() {
        assertEquals(HttpClient.class, underTest.getObjectType());
    }

    @Test
    public void shouldReturnAClosableHttpClient() throws Exception {
        assertTrue(CloseableHttpClient.class.isAssignableFrom(underTest.getObject().getClass()));
    }

    @Test
    public void shouldCallCloseWhenClosingDown() throws Exception {
        HttpClient httpClient = mock(CloseableHttpClient.class);
        ReflectionTestUtils.setField(underTest, "singletonInstance", httpClient);
        underTest.destroy();
        verify(httpClient).getClass();
    }
}
