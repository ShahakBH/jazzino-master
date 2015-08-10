-- attempt to clean up google check promotion data
-- promotion id wasn't logged correctly.
-- This script sets the promo id to null for default packages
-- and sets the promo id to 0 for promoted purchases that don't have a promo id
-- the data will still be still be slightly incorrect for promoted purchases, but at least you can identify a promoted purchase by testing for null.
-- going forward the data will be correct.

update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 3 and amount_chips = 5000;
update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 8 and amount_chips = 15000;
update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 15 and amount_chips = 30000;
update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 30 and amount_chips = 70000;
update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 70 and amount_chips = 200000;
update external_transaction set promo_id = null where cashier_name = 'GoogleCheckout' and amount = 90 and amount_chips = 300000;

update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 3 and amount_chips > 5000 and promo_id is null;
update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 8 and amount_chips > 15000 and promo_id is null;
update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 15 and amount_chips > 30000 and promo_id is null;
update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 30 and amount_chips > 70000 and promo_id is null;
update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 70 and amount_chips > 200000 and promo_id is null;
update external_transaction set promo_id = 0 where cashier_name = 'GoogleCheckout' and amount = 90 and amount_chips > 300000 and promo_id is null;
