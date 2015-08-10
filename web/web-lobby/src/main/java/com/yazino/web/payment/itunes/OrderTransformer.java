package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;

/**
* Describes an object that transforms an {@link Order} into an {@link ExternalTransaction}.
*/
interface OrderTransformer<T extends Order> {

    ExternalTransaction transform(T order);

}
