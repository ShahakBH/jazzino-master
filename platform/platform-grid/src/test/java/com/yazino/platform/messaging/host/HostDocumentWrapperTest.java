package com.yazino.platform.messaging.host;

import com.yazino.platform.model.BaseSpaceObjectTest;
import org.junit.Test;

public class HostDocumentWrapperTest extends BaseSpaceObjectTest {

    public HostDocumentWrapperTest() {
        super(HostDocumentWrapper.class);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings({"ConstantConditions"})
    public void wrapperWillNotAcceptNullToBusinessConstructor() {
        new HostDocumentWrapper(null);
    }

}
