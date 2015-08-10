package com.yazino.bi.opengraph;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class AccessTokenStoreTest {
    private static final int MAXIMUM_CAPACITY = 5;
    private static final int LEAST_RECENTLY_ACCESSED = MAXIMUM_CAPACITY - 1;
    private AccessTokenStore underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new AccessTokenStore(MAXIMUM_CAPACITY);
    }

    @Test
    public void shouldDropLeastRecentlyUsedElementAfterInsertionIntoFullMap() {
        for (int i = 0; i < MAXIMUM_CAPACITY; i++) {
            underTest.storeAccessToken(keyFor(i), new AccessTokenStore.AccessToken(Integer.toString(i)));
            System.out.println("putting" + i);
        }
        //this proves it is LRU not FIFO
        touchValuesInOrder(4, 3, 2, 1, 0);
        underTest.storeAccessToken(keyFor(MAXIMUM_CAPACITY), new AccessTokenStore.AccessToken(asValue(MAXIMUM_CAPACITY)));


        assertThat(underTest.findByKey(keyFor(MAXIMUM_CAPACITY)).getAccessToken(), is(equalTo(asValue(MAXIMUM_CAPACITY))));

        assertThat(underTest.findByKey(keyFor(LEAST_RECENTLY_ACCESSED)), nullValue());

        for (int i = 0; i < LEAST_RECENTLY_ACCESSED; i++) {
            System.out.println(underTest.findByKey(keyFor(i)));
            assertThat(underTest.findByKey(keyFor(i)).getAccessToken(), is(CoreMatchers.equalTo(asValue(i))));
        }
    }

    private String asValue(final int maximumCapacity) {
        return Integer.toString(maximumCapacity);
    }

    private AccessTokenStore.Key keyFor(final int i) {
        return new AccessTokenStore.Key(BigInteger.valueOf(i), "gameType");
    }

    private void touchValuesInOrder(int... values) {
        for (int value : values) {
            assertThat(underTest.findByKey(keyFor(value)).getAccessToken(), is(CoreMatchers.equalTo(Integer.toString(value))));
            System.out.println("getting" + value);
        }
    }


    @Test
    @Ignore //this is here to test sizes of stores so we can guesstimate how big to init to.... enjoy!
    public void ShouldTestGiGigaNormousAmountsOfKeys() throws IOException {
        underTest = new AccessTokenStore(100000);
        getSizeOf();
        for (int i = 0; i < 100000; i++) {
            underTest.storeAccessToken(new AccessTokenStore.Key(BigInteger.valueOf(i + 1000000), "HIGH_STAKES"), new AccessTokenStore.AccessToken(
                            "AAAAAFNDdxq8BAFFLGHCViCGZC3jpnyKSZAu7hoefxJZAPKoh0uvZBaKZCFXEtT3F3iwukZC8tQZAZBYCNoKrX4ZCFRIax8FvsBwkMTEQV18iTMRdQJBYH3CLi")
            );
        }
        getSizeOf();
    }

    private byte[] getSizeOf() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(underTest);
        oos.flush();
        oos.close();
        bos.close();
        byte[] bytes = bos.toByteArray();
        System.out.println(bytes.length);
        return null;
    }
}
