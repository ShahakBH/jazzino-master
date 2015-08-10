package com.yazino.payment.worldpay.fx;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayExchangeRatesParser {
    private static final String RECORD_ID_COMPANY = "02";
    private static final String RECORD_ID_EXCHANGE_RATE = "49";
    private static final String RECORD_ID_TRAILER = "92";
    private static final int RECORD_ID_FIELD_LENGTH = 2;
    private static final int RECORD_COMPANY_LENGTH = 27;
    private static final int RECORD_TRAILER_LENGTH = 12;
    private static final int RECORD_EXCHANGE_RATE_LENGTH = 55;
    private static final int EXCHANGE_RATE_DECIMAL_PLACES = 7;

    private final DateTimeFormatter dateParser = DateTimeFormat.forPattern("ddMMyy");

    public Optional<WorldPayExchangeRates> parse(final InputStream sourceStream) throws IOException {
        notNull(sourceStream, "sourceStream may not be null");

        final Set<CompanyExchangeRates> parsedCompanies = new HashSet<>();

        try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(sourceStream))) {
            int currentRecord = 0;
            Company currentCompany = null;
            final Set<ExchangeRate> parsedExchangeRates = new HashSet<>();

            String currentLine;
            while ((currentLine = inputReader.readLine()) != null) {
                final String trimmedLine = currentLine.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }
                if (trimmedLine.length() < RECORD_ID_FIELD_LENGTH) {
                    throw new ParseException("Malformed line: missing record ID in '%s'", currentLine);
                }

                ++currentRecord;

                final String recordId = trimmedLine.substring(0, 2);
                switch (recordId) {
                    case RECORD_ID_COMPANY:
                        currentCompany = parseCompany(trimmedLine, currentRecord);
                        break;
                    case RECORD_ID_EXCHANGE_RATE:
                        parsedExchangeRates.add(parseExchangeRate(trimmedLine, currentRecord));
                        break;
                    case RECORD_ID_TRAILER:
                        validateTrailer(trimmedLine, currentRecord, parsedExchangeRates);
                        if (currentCompany == null) {
                            throw new ParseException("Malformed file: trailer without header: %s", currentLine);
                        }
                        parsedCompanies.add(new CompanyExchangeRates(currentCompany.getCompanyNumber(), currentCompany.getProcessingDate(),
                                currentCompany.getAgreementDate(), parsedExchangeRates));
                        parsedExchangeRates.clear();
                        currentRecord = 0;
                        currentCompany = null;
                        break;
                    default:
                        throw new ParseException("Malformed line: unknown record ID %s in '%s'", recordId, currentLine);
                }
            }
        }

        if (parsedCompanies.isEmpty()) {
            return Optional.absent();
        }
        return Optional.fromNullable(new WorldPayExchangeRates(parsedCompanies));
    }

    private void validateTrailer(final String record, final int expectedRecordNumber, final Set<ExchangeRate> parsedExchangeRates) throws ParseException {
        validateRecordNumber(record, expectedRecordNumber, RECORD_TRAILER_LENGTH);

        final long expectedRateCount = parseNumber(record.substring(9, 12), "exchangeRateRecordCount", record);
        if (expectedRateCount != parsedExchangeRates.size()) {
            throw new ParseException("Malformed file: expected %d exchange rates, found %d in %s", expectedRateCount, parsedExchangeRates.size(), record);
        }
    }

    private Company parseCompany(final String record, final int expectedRecordNumber) throws ParseException {
        validateRecordNumber(record, expectedRecordNumber, RECORD_COMPANY_LENGTH);

        return new Company(parseNumber(record.substring(15, 21), "companyNumber", record),
                parseDate(record.substring(9, 15), "processingDate", record),
                parseDate(record.substring(21, 27), "agreementDate", record));
    }

    private ExchangeRate parseExchangeRate(final String record, final int expectedRecordNumber) throws ParseException {
        validateRecordNumber(record, expectedRecordNumber, RECORD_EXCHANGE_RATE_LENGTH);

        final BigDecimal exchangeRate;
        try {
            exchangeRate = new BigDecimal(record.substring(42, 55)).movePointLeft(EXCHANGE_RATE_DECIMAL_PLACES);
        } catch (NumberFormatException e) {
            throw new ParseException("Malformed file: field exchangeRate has invalid number %s in %s", record.substring(42, 55), record);
        }

        return new ExchangeRate(record.substring(9, 12),
                record.substring(12, 42).trim(),
                exchangeRate);
    }

    private void validateRecordNumber(final String record, final int expectedRecordNumber, final int expectedRecordLength) throws ParseException {
        if (record.length() != expectedRecordLength) {
            throw new ParseException("Malformed line: record is of an incorrect length in %s", record);
        }

        final long recordNumber = parseNumber(record.substring(2, 9), "recordNumber", record);
        if (recordNumber != expectedRecordNumber) {
            throw new ParseException("Malformed file: record number %d does not match expected value %d", recordNumber, expectedRecordNumber);
        }
    }

    private long parseNumber(final String number, final String fieldName, final String record) throws ParseException {
        try {
            return Long.parseLong(number);

        } catch (NumberFormatException e) {
            throw new ParseException("Malformed line: field %s has invalid number %s in %s", fieldName, number, record);
        }
    }

    private DateTime parseDate(final String date, final String fieldName, final String record) throws ParseException {
        try {
            return dateParser.parseDateTime(date);

        } catch (IllegalArgumentException e) {
            throw new ParseException("Malformed record: field %s has invalid date %s in %s", fieldName, date, record);
        }
    }

    private final class Company {
        private final long companyNumber;
        private final DateTime processingDate;
        private final DateTime agreementDate;

        private Company(final long companyNumber, final DateTime processingDate, final DateTime agreementDate) {
            this.companyNumber = companyNumber;
            this.processingDate = processingDate;
            this.agreementDate = agreementDate;
        }

        private long getCompanyNumber() {
            return companyNumber;
        }

        private DateTime getProcessingDate() {
            return processingDate;
        }

        private DateTime getAgreementDate() {
            return agreementDate;
        }
    }

}
