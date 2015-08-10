package com.yazino.payment.worldpay.emis;

import au.com.bytecode.opencsv.CSVReader;
import com.yazino.payment.worldpay.fx.ParseException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayChargebacksParser {

    private static final int COLUMN_RECORD_TYPE = 0;

    private static final int COLUMN_HEADER_FILE_TYPE = 3;
    private static final int COLUMN_HEADER_FILE_DATE = 11;

    private static final int COLUMN_RECORD_ACQUIRER_REF = 1;
    private static final int COLUMN_RECORD_CARD_NUMBER = 2;
    private static final int COLUMN_RECORD_CARD_SCHEME = 3;
    private static final int COLUMN_RECORD_MERCHANT_ID = 4;
    private static final int COLUMN_RECORD_TRANSACTION_REF = 5;
    private static final int COLUMN_RECORD_TRANSACTION_DATE = 6;
    private static final int COLUMN_RECORD_AMOUNT = 7;
    private static final int COLUMN_RECORD_CURRENCY = 8;
    private static final int COLUMN_RECORD_CARD_CENTRE_REF = 9;
    private static final int COLUMN_RECORD_CHARGEBACK_REASON = 10;
    private static final int COLUMN_RECORD_PROCESSING_DATE = 11;
    private static final int COLUMN_RECORD_TRANSACTION_TYPE = 12;
    private static final int COLUMN_RECORD_CHARGEBACK_AMOUNT = 13;
    private static final int COLUMN_RECORD_CHARGEBACK_REASON_CODE = 14;

    private static final int COLUMN_TRAILER_TOTAL_AMOUNT = 7;
    private static final int COLUMN_TRAILER_RECORD_COUNT = 13;

    private static final String FILETYPE_CHARGEBACK = "C";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("ddMMyy");
    private static final int SHORT_DATE_FORMAT_LENGTH = 5;

    public WorldPayChargebacks parse(final InputStream sourceStream) throws IOException {
        notNull(sourceStream, "sourceStream may not be null");

        CSVReader reader = null;

        try {
            reader = new CSVReader(new InputStreamReader(sourceStream));

            DateTime fileDate = null;
            final List<Chargeback> chargebacks = new ArrayList<>();

            int lineNumber = 1;
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length == 0 || isBlank(nextLine[COLUMN_RECORD_TYPE])) {
                    continue;
                }

                switch (nextLine[COLUMN_RECORD_TYPE]) {
                    case "0":
                    case "00":
                        fileDate = parseHeader(nextLine);
                        break;

                    case "5":
                    case "05":
                        chargebacks.add(parseChargeback(nextLine, lineNumber));
                        break;

                    case "99":
                        parseTrailer(nextLine, chargebacks, lineNumber);
                        break;

                    default:
                        throw new ParseException("Malformed line: unknown record ID '%s' in '%s'", nextLine[0], lineNumber);
                }
                ++lineNumber;
            }

            if (fileDate == null) {
                throw new ParseException("Malformed file: no data present or no header");
            }

            return new WorldPayChargebacks(chargebacks, fileDate);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private void parseTrailer(final String[] line,
                              final List<Chargeback> chargebacks,
                              final int lineNumber) throws ParseException {
        if (line.length < COLUMN_TRAILER_RECORD_COUNT + 1) {
            throw new ParseException("Malformed trailer: expected at least %d columns; found %d",
                    COLUMN_TRAILER_RECORD_COUNT + 1, line.length);
        }

        final int expectedRecords = parseInt(line[COLUMN_TRAILER_RECORD_COUNT].trim());
        if (expectedRecords != chargebacks.size()) {
            throw new ParseException("Malformed file: expected %d records; found %d", expectedRecords, chargebacks.size());
        }

        if (!chargebacks.isEmpty()) {
            final BigDecimal expectedTotalAmount = amountFrom(line[COLUMN_TRAILER_TOTAL_AMOUNT], chargebacks.get(0).getCurrency(), lineNumber);

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Chargeback chargeback : chargebacks) {
                totalAmount = totalAmount.add(chargeback.getChargebackAmount());
            }
            if (expectedTotalAmount.compareTo(totalAmount) != 0) {
                throw new ParseException("Malformed file: expected %s total refunds; found %s", expectedTotalAmount, totalAmount);
            }
        }
    }

    private Chargeback parseChargeback(final String[] line, final int lineNumber) throws ParseException {
        if (line.length < COLUMN_RECORD_CHARGEBACK_REASON_CODE) {
            throw new ParseException("Malformed record at line %d: expected at least %d columns; found %d",
                    lineNumber, COLUMN_RECORD_CHARGEBACK_REASON_CODE, line.length);
        }

        String chargebackReasonCode = null;
        if (line.length >= COLUMN_RECORD_CHARGEBACK_REASON_CODE + 1) {
            chargebackReasonCode = line[COLUMN_RECORD_CHARGEBACK_REASON_CODE].trim();
        }

        final Currency currency = currencyForNumericCode(line[COLUMN_RECORD_CURRENCY], lineNumber);
        return new Chargeback(new BigDecimal(line[COLUMN_RECORD_ACQUIRER_REF].trim()),
                line[COLUMN_RECORD_CARD_NUMBER].trim(),
                CardScheme.fromCode(line[COLUMN_RECORD_CARD_SCHEME].trim()),
                line[COLUMN_RECORD_MERCHANT_ID].trim(),
                line[COLUMN_RECORD_TRANSACTION_REF].trim(),
                parseDate(line[COLUMN_RECORD_TRANSACTION_DATE], lineNumber),
                amountFrom(line[COLUMN_RECORD_AMOUNT], currency, lineNumber),
                currency,
                line[COLUMN_RECORD_CARD_CENTRE_REF].trim(),
                line[COLUMN_RECORD_CHARGEBACK_REASON].trim(),
                parseDate(line[COLUMN_RECORD_PROCESSING_DATE], lineNumber),
                ChargebackTransactionType.fromCode(line[COLUMN_RECORD_TRANSACTION_TYPE].trim()),
                amountFrom(line[COLUMN_RECORD_CHARGEBACK_AMOUNT], currency, lineNumber),
                chargebackReasonCode);
    }

    private BigDecimal amountFrom(final String value,
                                  final Currency currency,
                                  final int lineNumber) throws ParseException {
        try {
            return new BigDecimal(value.trim()).movePointLeft(currency.getDefaultFractionDigits());
        } catch (NumberFormatException e) {
            throw new ParseException("Malformed record at line %d: invalid number %s", lineNumber, value);
        }
    }

    private Currency currencyForNumericCode(final String numericCurrencyCode,
                                            final int lineNumber) throws ParseException {
        try {
            final int currencyCode = Integer.parseInt(numericCurrencyCode.trim());
            for (Currency currency : Currency.getAvailableCurrencies()) {
                if (currency.getNumericCode() == currencyCode) {
                    return currency;
                }
            }
            throw new ParseException("Malformed record at line %d: invalid currency code %d", lineNumber, currencyCode);

        } catch (NumberFormatException | ParseException e) {
            throw new ParseException("Malformed record at line %d: invalid numeric currency code %s", lineNumber, numericCurrencyCode);
        }
    }

    private DateTime parseHeader(final String[] line) throws ParseException {
        if (line.length < COLUMN_HEADER_FILE_DATE + 1) {
            throw new ParseException("Malformed header: expected at least %d columns; found %d",
                    COLUMN_HEADER_FILE_DATE + 1, line.length);
        }
        if (!"C".equals(line[COLUMN_HEADER_FILE_TYPE].trim())) {
            throw new ParseException("File type is %s; expected %s", line[COLUMN_HEADER_FILE_TYPE], FILETYPE_CHARGEBACK);
        }
        return parseDate(line[COLUMN_HEADER_FILE_DATE], 1);
    }

    private DateTime parseDate(final String date, final int lineNumber) throws ParseException {
        String trimmedDate = date.trim();
        if (trimmedDate.length() == SHORT_DATE_FORMAT_LENGTH) {
            trimmedDate = "0" + trimmedDate;
        }
        try {
            return DateTime.parse(trimmedDate, DATE_FORMAT);
        } catch (IllegalArgumentException e) {
            throw new ParseException("Malformed record at line %d: invalid date %s", lineNumber, date);
        }
    }
}
