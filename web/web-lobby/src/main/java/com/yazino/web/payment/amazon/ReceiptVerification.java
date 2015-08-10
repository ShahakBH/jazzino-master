package com.yazino.web.payment.amazon;

import java.io.IOException;

public interface ReceiptVerification {

    VerificationResult verify(String userId, String purchaseToken) throws IOException;
}
