package com.yazino.payment.worldpay;

import com.google.common.base.Optional;

public enum MessageCode {
    // STLink
    OKAY(true, 100, "Ok", null),
    TRANSACTION_DOES_NOT_EXIST(false, 200, "Transaction does not exist.", null),
    FIELDS_MISSING(false, 300, "Field(s) Missing (Basic).", null),
    WRONG_CREDENTIALS(false, 302, "Wrong MerchantId, User Name or Password.", null),
    PLEASE_SEND_AGAIN(false, 303, "DB/System undefined errors or packet loss during transmission - Please send again.", null),
    TRANSMISSION_TIME_OUT(false, 304, "Time out during Transmission - Please send again.", null),
    METHOD_NOT_SUPPORTED(false, 305, "HTTP(S) method not supported – Please call support.", null),
    BATCH_TRANSMISSION_ERROR(false, 306, "Batch Transmission Error - Failed to write to file.", null),
    INVALID_DOCUMENT(false, 307, "Invalid document or Wrong XML Format.", null),
    WRONG_FORMAT_BASIC(false, 400, "Wrong Format (Basic).", null),
    WRONG_FORMAT_TX(false, 401, "Wrong Format (Transaction Specific).", null),
    WRONG_FORMAT_GP(false, 402, "(GP) Wrong Format or Parameter(s) Missing.", null),
    WRONG_FORMAT_BR(false, 403, "BR: Wrong Format or Parameter(s) Missing.", null),
    TRANSACTION_NOT_ALLOWED(false, 499, "Transaction not allowed.", null),
    TRANSACTION_TYPE_NOT_SUPPORTED(false, 500, "Transaction Type not Supported.", null),
    SECURITY_ERROR(false, 900, "Security ERROR - Please call Support.", null),

    // PaymentTrust
    NO_ANSWER(false, 2000, "No answer.", "Received no answer from banking network. Resend transaction."),
    DROPPED_TRANSACTION(false, 2001, "Dropped the transaction.", "No need to do this transaction."),
    PENDING_FINALISATION(false, 2040, "Pending to be finalized.", "Request submitted and waiting for Finalization."),
    PENDING(true, 2050, "Request pending.", "Request submitted and waiting for processing to be completed next cycle."),
    PENDING_WITH_ERRORS(false, 2051, "Request Pending with Errors.", "Cannot find the BTID for the original request."),
    NOTIFICATION_RECEIVED(false, 2053, "Notification Received.", "Notification Received."),
    PENDING_AT_PROCESSOR(false, 2055, "Request Pending at processor.", "The request in a pending state at the payment provider."),
    VALIDATION_ERROR(false, 2061, "Validation/Verification Failure.", "Validation/Verification Failure."),
    INVALID_IDENTIFICATION(false, 2062, "Invalid identification.", "Identification supplied is invalid."),
    VOIDED(false, 2080, "Voided.", "Voided."),
    APPROVED(true, 2100, "Transaction Approved.", "Transaction Authorized/Approved."),
    VALIDATED(false, 2101, "Validated.", "Validated."),
    VERIFIED(false, 2102, "Verified.", "Verified."),
    PRENOTED(false, 2103, "Prenoted.", "Prenoted."),
    APPROVED_BASE24(false, 2104, "Transaction approved.", "Transaction was approved - Base 24."),
    NOTIFICATION_CLEARED(false, 2105, "Notification Cleared.", "Notification Cleared."),
    CHECK_WARRANTED(false, 2106, "Check warranted.", "Certegy warrants the item presented."),
    CANCELLED_PLEASE_RESEND(false, 2112, "The original transaction is cancelled, please resend.", "The request is duplication of original transaction, it is voided."),
    CHECK_VERIFIED(false, 2122, "Check verified, not ACH-able, Auth Only.", "Check verified, not ACH-able, Auth Only."),
    BALANCE_INFO_OBTAINED(false, 2135, "ACH bank Balance info obtained.", "ACH bank Balance info obtained."),
    DEPOSITED_SUCCESSFULLY(false, 2150, "Funds deposited successfully.", "Deposit request previously submitted has been processed successfully."),
    REFUNDED_SUCCESSFULLY(false, 2160, "Funds refunded successfully.", "Refund request previously submitted has been processed successfully."),
    CANCELLED_SUCCESSFULLY_BY_REQUEST(true, 2170, "Transaction cancelled successfully.", "Cancellation request has been processed successfully."),
    VOIDED_SUCCESSFULLY(false, 2180, "Transaction voided successfully.", "Transaction voided successfully."),
    NOT_AUTHORISED(false, 2200, "Transaction NOT Authorized.", "Transaction Declined/Not Authorized/Not Settled."),
    ACQUIRER_DOES_NOT_ALLOW(false, 2201, "Acquirer/Issuer does not allow this transaction.", "Acquirer/Issuer does not allow this transaction."),
    CANCELLATION_DECLINED(false, 2202, "Cancellation declined.", "Cancellation declined by issuer/acquirer."),
    CANCELLATION_FAILED(false, 2203, "Cancellation cannot be performed.", "Cancellation transaction failed."),
    SOFT_AVS(false, 2204, "Soft AVS.", "Card was authorized but AVS did not match. Contact client."),
    SERVICE_PROVIDER_DOES_NOT_ALLOW(false, 2205, "Service provider does not allow this transaction.", "Service provider does not allow this transaction."),
    CURRENCY_DOES_NOT_MATCH_SYSTEM_STORED(false, 2206, "Invalid currency.", "Incoming record currency type does not match system stored currency."),
    INVALID_MERCHANT_ACCOUNT(false, 2208, "Invalid merchant account number.", "Invalid merchant account number."),
    INVALID_CARD_NUMBER(false, 2210, "Invalid card number.", "Bad check digit, length, or other credit card problem."),
    INVALID_CARD_EXPIRATION(false, 2212, "Invalid card expiration date.", "Card has expired or incorrect date entered. Confirm date."),
    CARD_EXPIRED(false, 2214, "Card expired.", "Card has expired."),
    INVALID_AMOUNT_ZERO_OR_UNREADABLE(false, 2216, "Invalid amount.", "Amount sent was 0 or unreadable."),
    INVALID_PAYMENT_METHOD(false, 2218, "Invalid method of payment.", "Method of payment is invalid for this account number."),
    CARD_NOT_VALID(false, 2219, "Card is not valid for this transaction.", "The specific card will not accept payment."),
    INVALID_METHOD_FOR_MERCHANT(false, 2220, "Invalid method of payment for merchant account number.", "Method of payment is invalid for this merchant."),
    INVALID_FIELD_DATA(false, 2222, "Invalid field data.", "Invalid information entered."),
    NO_SORT_CODE_OR_ACCOUNT(false, 2223, "No Sort code or Account Number in Payback system.", "No Sort code or Account Number in Payback system."),
    DATA_INACCURATE(false, 2224, "Data is inaccurate or missing.", "Specific and relevant data within transaction is inaccurate or missing."),
    DUPLICATED_TRANSACTION_SUBMITTED(false, 2226, "Duplicated transaction.", "Same transaction had been submitted."),
    ISSUER_DOES_NOT_ALLOW(false, 2228, "Invalid transaction.", "Issuer does not allow this transaction."),
    ONLY_ONE_DEPOSIT_PER_AUTHORISATION(false, 2229, "Invalid transaction.", "Processor permits only one deposit request per authorization."),
    INVALID_MERCHANT_ACCOUNT2(false, 2230, "Invalid merchant account number.", "Invalid merchant account number."),
    INVALID_ISSUER_OR_INSTITUTION(false, 2232, "Invalid issuer.", "Invalid issuer or institution."),
    INVALID_RESPONSE_CODE(false, 2234, "Invalid response code.", "Invalid response code."),
    CURRENCY_CODE_MISMATCH(false, 2235, "Invalid Currency Code Entered.",
            "Currency code submitted is different than code submitted with original authorization request."),
    INVALID_FOR_CREDIT(false, 2236, "Invalid for credit.", "Invalid for credit."),
    INVALID_REFUND_NOT_ALLOWED(false, 2237, "Invalid refund not allowed (CFT).", "Invalid refund not allowed (CFT)."),
    INVALID_FOR_DEBIT(false, 2238, "Invalid for debit.", "Invalid for debit."),
    INVALID_SEC_CODE(false, 2240, "Invalid SEC code – Amex.", "Amex CID is incorrect."),
    HONOUR_WITH_ID(false, 2242, "Honour with ID.", "Honour with ID."),
    INVALID_TRANSACTION(false, 2248, "Invalid Transaction", "Invalid Transaction"),
    INCORRECT_START_DATE(false, 2280, "Incorrect start date.", "Switch/Solo - Incorrect start date or requires an issue number. Please correct."),
    INVALID_ISSUE_NUMBER(false, 2282, "Invalid issue number.", "Switch/Solo - 1-digit number submitted when 2-digit number should have been sent. Please correct."),
    FORMAT_ISSUE(false, 2284, "Invalid transaction.", "Switch/Solo - a format issue, re-examine transaction layout. Please correct."),
    SWITCH_BANK_NOT_SUPPORTED(false, 2286, "Bank not supported by Switch.", "Bank not supported by Switch."),
    CARD_DOES_NOT_EXISTS(false, 2300, "Card does not exist.", "No card record."),
    INVALID_ROUTING_NUMBER(false, 2302, "Invalid transit routing number (ABA code).", "Invalid bank routing number."),
    MISSING_NAME(false, 2304, "Missing name.", "Missing the check writer’s name."),
    BANK_ACCOUNT_CLOSED(false, 2306, "Bank account closed.", "Bank account has been closed."),
    INVALID_ACCOUNT_TYPE(false, 2308, "Invalid account type.", "Account type is invalid or missing. Deposit transactions only."),
    ACCOUNT_DOES_NOT_EXIST(false, 2310, "Account does not exist.", "Account does not exist."),
    NO_ACCOUNT(false, 2312, "No account.", "Account number does not correspond to the individual."),
    ACCOUNT_HOLDER_DECEASED(false, 2314, "Account holder deceased.", "Account holder deceased. No further debits will be accepted by the bank."),
    BENEFICIARY_DECEASED(false, 2316, "Beneficiary deceased.", "Beneficiary deceased. No further debits will be accepted by the bank."),
    ACCOUNT_FROZEN(false, 2318, "Account frozen.", "The funds in this account are unavailable. No further debits will be accepted by the bank."),
    CUSTOMER_OPT_OUT(false, 2320, "Customer opt out.", "Customer has refused to allow the transaction."),
    BANK_REFUSED_ACH(false, 2322, "ACH non-participant.", "Banking institute does not accept ACH transactions (For US ECP)."),
    INVALID_ACCOUNT_NUMBER(false, 2324, "Invalid account number.", "Account number is incorrect."),
    AUTHORISATION_REVOKED(false, 2326, "Authorization revoked by customer.", "Customer has notified their bank not to accept these transactions."),
    CUSTOMER_ADVISED_NOT_AUTHORISED(false, 2328, "Customer advises not authorized.", "Customer has not authorized bank to accept these transactions."),
    INVALID_CECP_CODE(false, 2330, "Invalid CECP action code.", "Pertains to Canadian ECP only."),
    INVALID_ACCOUNT_NUMBER_FORMAT(false, 2332, "Invalid account number format.",
            "Format of account number does not pass check digit routine for that institution. (For CDN ECP)."),
    BAD_ACCOUNT_NUMBER_DATA(false, 2334, "Bad account number data.", "Invalid characters in account number."),
    CARD_SURPASSED_DAILY_LIMIT(false, 2350, "Card surpassed daily limit.", "Card has surpassed daily transaction amount limit."),
    SURPASSED_DAILY_LIMIT(false, 2351, "Supassed daily limit.", "Surpassed daily transaction amount limit."),
    TIME_CARD_USED_LIMIT(false, 2352, "Times card used limit.", "The limit of number of times used for the card has been surpassed."),
    OVER_CREDIT_LIMIT(false, 2354, "Over credit limit.", "Card has surpassed its credit limit."),
    ENTER_LESSER_AMOUNT(false, 2356, "Enter lesser amount.", "Enter a lesser amount."),
    TRY_LESSER_AMOUNT(false, 2357, "Try Lesser Amount / Whole Dollar Only.", "Try Lesser Amount / Whole Dollar Only."),
    NO_CREDIT_AMOUNT(false, 2358, "No credit amount.", "No credit amount."),
    ONE_PURCHASE_LIMIT(false, 2360, "One purchase limit.", "Card is limited to one purchase."),
    OVER_SAV_LIMIT(false, 2362, "Over Sav limit.", "Over Sav limit."),
    OVER_SAV_FREQUENCY(false, 2364, "Over Sav frequency.", "Over Sav frequency."),
    CARD_NOT_SUPPORTED(false, 2366, "Card not supported.", "Card not supported."),
    INVALID_PIN(false, 2368, "Invalid PIN.", "Invalid PIN."),
    ALLOWABLE_PIN_TRIES_EXCEEDED(false, 2370, "Allowable PIN tries exceeded.", "Allowable PIN tries exceeded."),
    PIN_REQUIRED(false, 2372, "PIN required.", "PIN required."),
    CARD_FAILED_MOD10(false, 2374, "Card failed MOD 10 check.", "Card failed MOD 10 check verification."),
    ON_NEGATIVE_FILE(false, 2380, "On negative file.", "Account number appears on negative file."),
    STOP_PAYMENT_ISSUED(false, 2382, "Stop Payment Issued.", "Stop Payment Issued."),
    ENTER_WHOLE_DOLLARY_AMOUNT(false, 2384, "Enter Whole Dollar Amount.", "Enter Whole Dollar Amount."),
    UNAUTHORISED_USAGE(false, 2386, "Unauthorized Usage.", "Unauthorized Usage."),
    PLTF_FULL(false, 2400, "PTLF full.", "PTLF full."),
    FRAUD_SUSPECTED(false, 2401, "Fraud suspected.", "Fraud suspected."),
    UNABLE_TO_PROCESS(false, 2402, "Unable to process transaction.", "Unable to process transaction."),
    DUPLICATE_TRANSACTION(false, 2403, "Duplicate transaction.", "Duplicate transaction."),
    CUTOFF_IN_PROGRESS(false, 2404, "Cutoff in progress.", "Cutoff in progress."),
    INCORRECT_PIN(false, 2405, "Incorrect PIN.", "Incorrect PIN."),
    PIN_TRIES_EXCEEDED(false, 2406, "PIN tries exceeded.", "PIN tries exceeded."),
    EXCEEDS_WITHDRAWAL_FREQUENCY(false, 2407, "Exceeds withdrawal frequency.", "Exceeds withdrawal frequency."),
    INVALID_3D_SECURE_DATA(false, 2410, "Invalid 3D Secure Data.", "Invalid 3D Secure Data."),
    MULTIPLE_ERRORS(false, 2420, "Multiple errors.", "There is more than one error."),
    VALIDATION_FAILS_INTERNAL_CHECK(false, 2430, "Validation Fails Internal Check.", "Validation Fails Internal Check."),
    VALIDATION_FAILS_NAME_CHECK(false, 2431, "Validation Fails Name Check.", "Validation Fails Name Check."),
    VALIDATION_FAILS_ROUTING_CHECK(false, 2432, "Validation Fails Routing Check.", "Validation Fails Routing Check."),
    FIRST_OR_LAST_NAME_INVALID(false, 2440, "FirstName or LastName Invalid.", "FirstName or LastName Invalid."),
    BANK_TIMEOUT_ERROR_HOST(false, 2610, "Bank Timeout error / Re-Send.", "Timeout waiting for host response."),
    BANK_TIMEOUT_ERROR_INTERNAL(false, 2611, "Bank Timeout error /Re-Send.", "Internal timeout."),
    AUTHENTICATION_SYSTEM_DOWN(false, 2612, "Authorization host system down or unavailable.", "Authorization host system is temporarily unavailable."),
    ACQUIRER_CANNOT_PROCESS_RETRY(false, 2613, "Acquirer Cannot Process Transaction at This Time. Please Retry.",
            "Acquirer Cannot Process Transaction at This Time. Please Retry."),
    ACQUIRER_UNAVAILABLE(false, 2614, "Acquirer/Issuer unavailable. Resend.",
            "Authorization host network could not reach the bank, which issued the card or Acquirer."),
    BANK_TIMEOUT_ERROR(false, 2615, "Bank Timeout error / Re-Send", "Bank Timeout error / Re-Send"),
    INVALID_ISSUER(false, 2616, "Invalid issuer.", "Invalid issuer or institution."),
    UNIDENTIFIED_ERROR(false, 2618, "Unidentified error.", "Unidentified error. Unable to process transaction."),
    UNABLE_TO_PROCESS_SYSTEM_MALFUNCTION(false, 2620, "Unable to process.", "Unable to process transaction due to system malfunction."),
    UNABLE_TO_AUTHORISE(false, 2622, "Unable to authorize.", "Unable to authorize due to system malfunction."),
    MERCHANT_INFORMATION_INCOMPLETE(false, 2624, "Merchant information incomplete.", "Merchant information incomplete."),
    INVALID_CVN_VALUE(false, 2626, "Invalid CVN value.", "Invalid CVN value."),
    INVALID_TRACK2_DATA(false, 2627, "Invalid track2 data.", "The track2 format information is incorrect."),
    TRANSACTION_NOT_SUPPORTED(false, 2628, "Transaction not supported.", "Merchant not Support this transaction."),
    INVALID_STORE_ID(false, 2630, "Invalid store ID.", "No such store ID for the merchant."),
    INVALID_AUTHCODE(false, 2632, "Invalid authcode.", "Invalid authcode."),
    INVALID_FORMAT(false, 2634, "Invalid format.", "Invalid format."),
    INVALID_MESSAGE_TYPE(false, 2636, "Invalid message type.", "Invalid message type."),
    INVALID_POS_SYSTEM_TYPE(false, 2638, "Invalid POS system type.", "Invalid POS system type."),
    TRANSACTION_HAS_BEEN_CANCELLED(false, 2640, "This transaction has been cancelled.", "A message has be sent to reverse previous time out transaction."),
    TRXSOURCE_NOT_SUPPORTED(false, 2642, "This TrxSource is not supported by the bank.", "This TrxSource is not supported by the bank."),
    INSUFFICIENT_TERMINAL_IDS(false, 2644, "Insufficient Terminal IDs, please try again.", "Not enough Terminal IDs at the time of transaction."),
    ACQUIRER_CANNOT_PROCESS(false, 2646, "Acquirer cannot process transaction.", "Acquirer cannot process transaction."),
    RETAIN_CARD(false, 2648, "Retain card if possible.", "Retain card, no reason specified."),
    DOB_DOES_NOT_MATCH_RECORDS(false, 2649, "DOB Does not Match Records.", "DOB Does not Match Records."),
    RESUBMIT_WITH_DOC(false, 2650, "Resubmit with DOB.", "Resubmit with DOB."),
    INVALID_FILE(false, 2700, "Invalid file.", "General error for PC card."),
    AMOUNTS_DO_NOT_COMPUTE(false, 2702, "Amounts do not compute.", "Amount is invalid."),
    LINE_ITEMS_DO_NOT_MATCH_TOTAL(false, 2704, "Line items do not add up to summary total.", "Line items do not add up to summary total."),
    NOT_SUPPORTED_FOR_BATCH(false, 2706, "Not supported for batch.", "Not supported for batch."),
    MANDATORY_FIELD_MISSING(false, 2712, "Mandatory field is invalid or missing.", "Mandatory field is invalid or missing."),
    TOTAL_LINE_ITEMS_DO_NOT_ADD_UP(false, 2714, "Total line items do not add up.", "Total line items do not add up."),
    LINE_ITEMS_MISSING(false, 2716, "Line items missing.", "Line items missing."),
    COMMODITY_CODE_MISSING(false, 2718, "Commodity code is invalid or missing.", "Commodity code is invalid or missing."),
    CROSS_BORDER_MISSING(false, 2720, "Cross border information is invalid or missing.", "Cross border information is invalid or missing."),
    INVALID_PURCHASED_CARD_NUMBER(false, 2722, "Invalid purchase card number.", "Not a purchase card."),
    INVALID_ICC_PARAMETER(false, 2802, "Invalid ICC parameter.", "One of the ICC parameters submitted was invalid."),
    TRANSACTION_CANNOT_BE_FOUND(false, 2804, "Transaction cannot be found.", "The requested transaction was not found."),
    REQUEST_IN_PROCESS(false, 2830, "Request in progress", "Request in progress"),
    PARTIAL_APPROVAL(false, 2831, "Partial Approval", "Partial Approval"),
    RESTRICTED_CARD(false, 2832, "Restricted Card", "Restricted Card"),
    EXCEEDS_WITHDRAWAL_AMOUNT_LIMIT(false, 2833, "Exceeds Withdrawal Amount Limit", "Exceeds Withdrawal Amount Limit"),
    CANNOT_VERIFY_PIN(false, 2844, "Cannot Verify PIN", "Cannot Verify PIN"),
    NO_CASHBACK_ALLOWED(false, 2845, "No Cashback Allowed", "No Cashback Allowed"),
    SYSTEM_ERROR(false, 2846, "System Error", "System Error"),
    CHARGEBACK(false, 2847, "Chargeback", "Chargeback"),
    CANNOT_ROUTE_TO_ISSUER(false, 2848, "Cannot Route to Issuer", "Cannot Route to Issuer"),
    MAX_REFUND_REACHED(false, 2849, "Max Refund Reached", "Max Refund Reached"),
    OVER_FLOOR_LIMIT(false, 2850, "Over floor Limit", "Over floor Limit"),
    PICK_UP_CARD(false, 2952, "Pick up card.", "Card issuer wants card returned. Call issuer."),
    CARD_STOLEN(false, 2954, "Card stolen.", "Card reported as lost/stolen."),
    DO_NOT_HONOUR(false, 2956, "Do not honor.", "Generic decline. No other information is being provided by the issuer."),
    CALL_BANK(false, 2958, "Call Bank.", "Issuer wants voice contact with cardholder."),
    INSUFFICIENT_FUNDS(false, 2960, "Insufficient funds.", "Insufficient funds."),
    CVN_FAILURE(false, 2962, "CVN failure.", "Issuer has declined request because CVV2 edit failed."),
    DELINQUENT_ACCOUNT(false, 2964, "Delinquent account.", "Delinquent account."),
    LOAD_FAILED(false, 2966, "Load Failed.", "Prepaid card load failed."),
    LOAD_LIMIT_EXCEEDED(false, 2968, "Load Limit Exceeded.", "Prepaid card load limit exceeded."),
    VELOCITY_LIMIT_EXCEEDED(false, 2970, "Velocity Limit Exceeded.", "Velocity limit exceeded."),
    PERMISSION_DENIED(false, 2972, "Permission Denied.", "Prepaid card process permission denied."),
    INVALID_PREPAID_ACCOUNT_ID(false, 2974, "Invalid Account ID.", "Prepaid card invalid account ID."),
    CANCELLATION(false, 2990, "Cancellation.", "Cancellation is going to reverse the authorization."),
    TRANSACTION_PENDING(false, 3050, "Transaction pending.", "Transaction pending."),
    TRANSACTION_PENDING_RATE_ESCALATED(false, 3051, "Transaction pending with rate escalated.", "A new rate is assigned for the transaction."),
    TRANSACTION_PENDING_REFUND(false, 3052, "Transaction pending for refund.", "Transaction waiting for placement approve then refund."),
    FX_TRANSACTION_APPROVED(false, 3100, "Transaction approved.", "FX transaction approved."),
    RATE_ESCALATED(false, 3111, "Rate escalated.", "Transaction rate escalated."),
    CANCELLED_SUCCESSFULLY(false, 3170, "Transaction cancelled successfully.", "Transaction cancelled successfully."),
    REFUNDED(false, 3171, "Transaction refunded.", "Transaction refunded."),
    RATE_EXPIRED(false, 3200, "Rate expired.", "Rate requested has expired and no new rate is available."),
    CANCELLATION_CANNOT_BE_PERFORMED(false, 3203, "Cancellation cannot be performed.",
            "￼The deposit/refund transaction being cancelled cannot be because it has already been submitted."),
    CANCELLATION_NOT_ENABLED(false, 3204, "Cancellation not enabled.", "Cancellation disabled in merchant set-up."),
    INVALID_CURRENCY_OF_RECORD(false, 3206, "Invalid currency.", "Invalid currency of record."),
    EXCHANGE_CURRENCY_NOT_SUPPORTED(false, 3207, "Exchange currency not supported.", "Exchange currency not setup in merchant account."),
    CURRENCY_CONVERSION_REDUNDANT(false, 3208, "CurrencyId matches ConvertedCurrencyId.", "Conversion to same currency redundant."),
    CURRENCY_PAIR_NOT_SUPPORTED(false, 3209, "Currency pair not supported.", "Cannot convert to requested currency."),
    CURRENCY_DOES_NOT_MATCH_FX_REQUEST(false, 3210, "CurrencyId does not match FX request.", "Currency submitted does not match the original rate request."),
    INVALID_AMOUNT(false, 3216, "Invalid amount.", "Invalid amount."),
    INVALID_FX_ID(false, 3217, "Invalid FXID.", "FXID submitted is invalid."),
    ISSUER_NOT_AVAILABLE(false, 3218, "Issuer is not available. Please try again.", "Unexpected error."),
    CARD_NOT_VALID_FOR_TRANSACTION(false, 3219, "Credit card is not valid for this transaction.", "Credit card is not valid for this transaction."),
    CURRENCY_NOT_SUPPORTED(false, 3220, "Currency Not Supported.", "Currency of card not supported."),
    DATA_MISSING(false, 3224, "Data is inaccurate or missing.", "One or more required parameters are not present."),
    DUPLICATED_TRANSACTION(false, 3226, "Duplicated transaction.", "Duplicated transaction."),
    INVALID_TRANSACTION_GENERIC(false, 3228, "Invalid transaction.", "Generic error message for invalid transactions."),
    INVALID_ACCOUNT_DATA(false, 3321, "Invalid account data.", "Invalid account data."),
    NON_EXECUTABLE_RATE(false, 3341, "Non-executable rate.", "Quoted rate is not executable."),
    REFUND_OVER_LIMIT(false, 3354, "Refund amount over limit.", "Refund is over the original value of the deal."),
    RATE_QUOTE_INVALID(false, 3361, "Rate quote invalid.", "Quoted rate is invalid."),
    RATE_EXPIRED_NOT_ESCALATED(false, 3362, "Rate expired not escalated.", "Expired rate cannot be escalated."),
    RATE_REVOKED(false, 3371, "Rate revoked.", "Rate has been revoked."),
    TRANSACTION_EXCEEDS_AMOUNT_LIMIT(false, 3381, "Transaction exceeds amount limit.", "Transaction min/max limits reached."),
    BATCH_SIZE_EXCEEDS_MAXIMUM(false, 3391, "Batch size exceeds the maximum allowed.",
            "Batch size exceeds the Maximum allowable size transaction/payment not written to database."),
    FX_SYSTEM_UNAVAILABLE(false, 3614, "FX system unavailable.", "FX system cannot be reached."),
    REFUND_NOT_ENABLED(false, 3781, "Refund not enabled.", "Refund disabled in merchant set-up."),
    REFUND_NOT_POSSIBLE(false, 3783, "Refund not possible.", "Refund cannot be processed."),
    REFUND_BEYOND_TIME_PERIOD(false, 3785, "Refund beyond time period.", "Refund beyond maximum time period."),
    CARDHOLDER_ENROLLED(false, 4050, "Cardholder enrolled.", "Cardholder enrolled for 3D Secure."),
    CARDHOLDER_AUTHENTICATED(false, 4100, "Cardholder authenticated.", "Cardholder answered password/challenge question correctly."),
    CARDHOLDER_AUTH_ATTEMPTED(false, 4101, "Cardholder authentication attempted.", "Cardholder authentication attempted."),
    CARDHOLDER_NOT_ENROLLED(false, 4200, "Cardholder not enrolled.", "Cardholder not enrolled for 3D Secure."),
    CARDHOLDER_NOT_PARTICIPATING(false, 4202, "Card not participating in 3D Secure.", "Credit card is not recognized as a 3D Secure card."),
    CARDHOLDER_ENROLMENT_NOT_VERIFIED(false, 4203, "Cardholder enrolment not verified.", "Cardholder enrolment not verified."),
    CARDHOLDER_NOT_AUTHENTICATED(false, 4204, "Cardholder not authenticated.", "Cardholder failed to answer password/challenge question."),
    INVALID_CURRENCY(false, 4206, "Invalid currency.", "Invalid currency."),
    INVALID_MERCHANT_ACCOUNT_NUMBER(false, 4208, "Invalid merchant account number.", "Invalid merchant account number."),
    INVALID_CREDIT_CARD_NUMBER(false, 4210, "Invalid credit card number.", "Invalid credit card number."),
    INVALID_CREDIT_CARD_EXPIRY(false, 4212, "Invalid credit card expiration date.", "Invalid credit card expiration date."),
    INVALID_AMOUNT2(false, 4216, "Invalid amount.", "Invalid amount."),
    DATA_INACCURATE2(false, 4224, "Data is inaccurate or missing.", "Specific and relevant data within transaction is inaccurate or missing."),
    INVALID_TRANSACTION2(false, 4228, "Invalid transaction.", "Invalid transaction."),
    MERCHANT_NOT_PARTICIPATING(false, 4230, "Merchant Not Participating.", "Merchant Not Participating in 3D Secure."),
    CARDHOLDER_ENROLMENT_FAILED(false, 4240, "Cardholder enrolment failed.", "Enrolment process failed."),
    CARDHOLDER_AUTHENTICATION_FAILED(false, 4242, "Cardholder authentication failed.", "Authentication process failed."),
    MPI_NOT_AVAILABLE(false, 4614, "MPI not available.", "MPI not available."),
    DIRECTORY_NOT_AVAILABLE(false, 4616, "Directory server not available.", "Directory server not available."),
    INTERNAL_MPI_ERROR(false, 4618, "Internal MPI error.", "Internal MPI error."),
    INVALID_SECURE_ID(false, 4626, "Invalid SecureId.", "Invalid SecureId."),
    TRANSACTION_ALREADY_PROCESSED_3D_SECURE(false, 4700, "3D Secure transaction already processed.", "3D Secure transaction already processed."),
    REQUEST_PENDING(true, 7050, "Request Pending", "The order has been recorded in the redirect system."),
    TRANSACTION_APPROVED(true, 7100, "Transaction Approved", "Redirect has processed the order."),
    CUSTOMER_INFORMATION_ADDED(true, 7102, "Customer Information Added", "Customer information was added."),
    CUSTOMER_INFORMATION_UPDATED(true, 7104, "Customer Information Updated", "Customer information was updated."),
    INVALID_RECORD_CURRENCY(false, 7206, "Invalid Currency", "Incoming record currency type does not match system stored currency."),
    INVALID_MERCHANT_ACCOUNT_NUMBER2(false, 7208, "Invalid Merchant Account Number", "Invalid merchant account number."),
    INVALID_CREDIT_CARD_NUMBER2(false, 7210, "Invalid Credit Card Number", "Bad check digit, length, or other credit card problem."),
    INVALID_CARD_EXPIRY_DATE(false, 7212, "Invalid Credit Card Expiration Date", "Credit card has expired or incorrect date entered. Confirm date."),
    INVALID_AMOUNT_SENT(false, 7216, "Invalid Amount", "Amount sent was 0 or unreadable."),
    DATA_MISSING_OR_INACCURATE(false, 7224, "Data Is Inaccurate Or Missing", "Data is inaccurate or missing."),
    INVALID_TRANSACTION3(false, 7228, "Invalid Transaction.", "Invalid Transaction."),
    TOKEN_NOT_FOUND(false, 7230, "Token Not Found", "Token is not found or not available."),
    CARD_VERIFICATION_FAILED(false, 7240, "Card verification failed", "PT verify failed."),
    INCORRECT_START_DATE2(false, 7280, "Incorrect Start Date", "Switch/Solo - Incorrect start date or requires an issue number. Please correct."),
    INVALID_ISSUE_NUMBER2(false, 7282, "Invalid Issue Number", "Switch/Solo - 1-digit number submitted when 2-digit number should have been sent. Please correct."),
    NO_CARDS_FOUND(true, 7300, "No Cards Found", "No cards found for specified CustomerId."),
    NO_CUSTOMER_FOUND(false, 7310, "No Customer Found", "CustomerId is not registered in redirect system."),
    CARD_NOT_SUPPORTED2(false, 7366, "Card Not Supported", "Merchant does not support this card."),
    ACQUIRER_ISSUE_PROVIDER_UNAVAILABLE_RESEND(false, 7614, "Acquirer/Issuer/Provider unavailable. Resend", "The PT system does not answer."),
    TRANSACTION_NOT_SUPPORTED2(false, 7628, "Transaction Not Supported", "Merchant not Support this transaction."),
    ORDER_PERIOD_EXPIRED(false, 7785, "Order Period Expired", "The order could not be completed within the allotted time."),
    TRANSACTION_CANNOT_BE_FOUND2(false, 7804, "Transaction Cannot Be Found", "Transaction was not found in the RD system.");

    private final boolean successful;
    private final int code;
    private final String description;
    private final String explanation;

    private MessageCode(final boolean successful,
                        final int code,
                        final String description,
                        final String explanation) {
        this.successful = successful;
        this.code = code;
        this.description = description;
        this.explanation = explanation;
    }

    public static boolean isSuccessful(final Optional<String> messageCode) {
        if (messageCode.isPresent()) {
            final MessageCode parsedCode = forCode(messageCode.get());
            if (parsedCode != null) {
                return parsedCode.isSuccessful();
            }
        }
        return false;
    }


    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public static MessageCode forCode(final String codeToFindAsString) {
        if (codeToFindAsString == null) {
            return null;
        }

        final int codeToFind;
        try {
            codeToFind = Integer.parseInt(codeToFindAsString);
        } catch (NumberFormatException e) {
            return null;
        }

        for (MessageCode messageCode : values()) {
            if (messageCode.getCode() == codeToFind) {
                return messageCode;
            }
        }
        return null;
    }

    public static MessageCode forCode(final int codeToFind) {
        for (MessageCode messageCode : values()) {
            if (messageCode.getCode() == codeToFind) {
                return messageCode;
            }
        }
        return null;
    }
}
