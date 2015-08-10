package strata.server.worker.audit.persistence;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;

public class PostgresGameAuditDAOTest {
    PostgresGameAuditDAO underTest;
    @Before
    public void setUp() throws Exception {
        underTest = new PostgresGameAuditDAO(mock(JdbcTemplate.class));
    }

    @Test
    public void truncateShouldReduceSizeOfSimpleString(){
        Assert.assertThat(underTest.truncateAtMaxSize("abcdefghij123456",10), CoreMatchers.is(IsEqual.equalTo("abcdefghij")));
        Assert.assertThat(underTest.truncateAtMaxSize("abcdefghij123456",16), CoreMatchers.is(IsEqual.equalTo("abcdefghij123456")));
        Assert.assertThat(underTest.truncateAtMaxSize("abc",10), CoreMatchers.is(IsEqual.equalTo("abc")));
    }

    @Test
    public void truncateShouldReduceMultibyteStrings(){
        test("abcd", 0, 0);
        test("abcd", 1, 1);
        test("abcd", 2, 2);
        test("abcd", 3, 3);
        test("abcd", 4, 4);
        test("abcd", 5, 4);

        test("a\u0080b", 0, 0);
        test("a\u0080b", 1, 1);
        test("a\u0080b", 2, 1);
        test("a\u0080b", 3, 3);
        test("a\u0080b", 4, 4);
        test("a\u0080b", 5, 4);

        test("a\u0800b", 0, 0);
        test("a\u0800b", 1, 1);
        test("a\u0800b", 2, 1);
        test("a\u0800b", 3, 1);
        test("a\u0800b", 4, 4);
        test("a\u0800b", 5, 5);
        test("a\u0800b", 6, 5);

        // surrogate pairs
        test("\uD834\uDD1E", 0, 0);
        test("\uD834\uDD1E", 1, 0);
        test("\uD834\uDD1E", 2, 0);
        test("\uD834\uDD1E", 3, 0);
        test("\uD834\uDD1E", 4, 4);
        test("\uD834\uDD1E", 5, 4);
    }

    private void test(String s, int maxBytes, int expectedBytes) {
        String result = underTest.truncateAtMaxSize(s, maxBytes);
        byte[] utf8 = result.getBytes(Charset.forName("UTF-8"));
        if (utf8.length > maxBytes) {
            System.out.println("BAD: our truncation of " + s + " was too big");
        }
        if (utf8.length != expectedBytes) {
            System.out.println("BAD: expected " + expectedBytes + " got " + utf8.length);
        }
        System.out.println(s + " truncated to " + result);
        Assert.assertTrue(utf8.length<=maxBytes) ;
    }


}
