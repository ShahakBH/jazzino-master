package com.yazino.bi.operations.controller;

import com.yazino.platform.account.ExternalTransactionStatus;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.CurrencyRatesDefinition;
import com.yazino.bi.operations.persistence.PaymentTransactionReportDao;
import com.yazino.bi.operations.util.DataFormatHelper;
import com.yazino.bi.operations.model.ReportDefinitionCommand;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Controller managing the payment reports
 */
@Controller
public class PaymentTransactionController extends ControllerWithDateBinderAndFormatterAndReportFormat {
    private static final DateTimeFormatter FMT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String ALL = "";

    private final PaymentTransactionReportDao dao;
    private final CurrencyRatesDefinition ratesDefinition;

    /**
     * Initializes the controller
     *
     * @param dao Payment report DAO to use
     */
    @Autowired(required = true)
    public PaymentTransactionController(final PaymentTransactionReportDao dao,
                                        final CurrencyRatesDefinition ratesDefinition) {
        notNull(dao, "dao may not be null");
        notNull(ratesDefinition, "ratesDefinition may not be null");

        this.dao = dao;
        this.ratesDefinition = ratesDefinition;
    }

    /**
     * Returns the list of available payment methods
     *
     * @return Payment methods available from the DAO
     */
    @ModelAttribute("paymentMethods")
    public Map<String, String> getPaymentMethods() {
        final List<String> methodsList = dao.getAvailablePaymentMethods();
        final Map<String, String> returnedMethods = new LinkedHashMap<String, String>();
        returnedMethods.put("", "All methods");
        returnedMethods.put(PaymentTransactionReportDao.PURCHASE_TRANSACTIONS, "Purchases");
        returnedMethods.put(PaymentTransactionReportDao.OFFER_TRANSACTIONS, "Offers");
        for (final String method : methodsList) {
            returnedMethods.put(method, method);
        }
        return returnedMethods;
    }

    @RequestMapping(value = {"/paymentTransactionReportDefinition", "/report/paymentTransaction"}, method = RequestMethod.GET)
    public ModelAndView showReportForm() {
        final ReportDefinitionCommand command = new ReportDefinitionCommand();
        final DateTime toDate = new DateTime().minusDays(1);
        command.setToDate(toDate.toString(FMT));
        command.setFromDate(toDate.toString(FMT));
        command.setReportFormat("html");
        command.setPaymentMethod(ALL);
        command.setCurrencyCode(ALL);
        command.setTxnStatus(ALL);
        command.setPaymentTransactionReportType("all");

        return new ModelAndView("paymentTransactionReportDefinition")
                .addObject("command", command);
    }

    @ModelAttribute("currencyCodes")
    public Map<String, String> getCurrencyCodes() {
        final Map<String, String> codes = new LinkedHashMap<String, String>();
        codes.put("", "All currencies");
        for (final String code : ratesDefinition.getConversionRates().keySet()) {
            codes.put(code, code);
        }

        return codes;
    }

    @ModelAttribute("transactionStatuses")
    public Map<String, String> getTransactionStatuses() {
        final Map<String, String> statuses = new LinkedHashMap<String, String>();
        statuses.put("", "All statuses");
        statuses.put(PaymentTransactionReportDao.SUCCESSFUL_STATUS, "All successful");
        for (final ExternalTransactionStatus status : ExternalTransactionStatus.values()) {
            statuses.put(status.name(), status.name());
        }

        return statuses;
    }

    @ModelAttribute("searchOptions")
    public Map<String, String> getSearchOptions() {
        final Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("all", "All");
        options.put("txn", "By ID");
        return options;
    }

    @RequestMapping("/report/paymentTransaction/{internalTransactionId:.+}")
    public ModelAndView viewSingleTransaction(@PathVariable("internalTransactionId") final String internalTransactionId) {
        final ReportDefinitionCommand command = new ReportDefinitionCommand();
        command.setPaymentTransactionReportType("txn");
        command.setTransactionId(internalTransactionId);
        return processCommand(command);
    }

    @RequestMapping(value = {"/paymentTransactionReportDefinition", "/paymentTransactionReport", "/report/paymentTransaction"}, method = RequestMethod.POST)
    public ModelAndView processCommand(final ReportDefinitionCommand command) {
        List<PaymentTransactionData> paymentTransactionData;
        if ("all".equals(command.getPaymentTransactionReportType())) {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            final DateTime fromDate = dateTimeFormatter.parseDateTime(command.getFromDate());
            final DateTime toDate = dateTimeFormatter.parseDateTime(command.getToDate());
            final String currencyCode = command.getCurrencyCode();
            final String cashier = command.getPaymentMethod();
            final String txnStatus = command.getTxnStatus();

            paymentTransactionData =
                    dao.getPaymentTransactionData(fromDate, toDate, currencyCode, cashier, txnStatus);
        } else {
            paymentTransactionData = dao.getPaymentTransactionData(command.getTransactionId());
        }

        addGbpAmount(command, paymentTransactionData);

        return new ModelAndView("paymentTransactionReportDefinition")
                .addObject("command", command)
                .addObject(ReportConstants.REPORT_DATA_MODEL, paymentTransactionData);
    }

    private void addGbpAmount(final ReportDefinitionCommand command,
                              final List<PaymentTransactionData> paymentTransactionData) {
        final Map<String, Double> rates = ratesDefinition.getConversionRates();
        for (final PaymentTransactionData transactionData : paymentTransactionData) {
            final Double amount = transactionData.getAmount();
            final String currencyCode = transactionData.getCurrencyCode();
            if (amount != null && currencyCode != null && rates.containsKey(currencyCode)) {
                transactionData.setGbpAmount(amount / rates.get(currencyCode));
            }
        }
    }

    @ModelAttribute("formatter")
    public DataFormatHelper getFormatter() {
        return DataFormatHelper.getInstance();
    }
}
