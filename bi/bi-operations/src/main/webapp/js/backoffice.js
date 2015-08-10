/*global jQuery,document,console,$,FB*/

var playerDashboard = {};

(function () {
    var extractRequiredFacebookUserIdsFromInviteTable, replaceFacebookIdsWithNames;
    extractRequiredFacebookUserIdsFromInviteTable = function () {
        var ids = "", facebookId;
        $("#tabTable tr td:nth-child(2)").each(function (index, value) {
            facebookId = jQuery.trim(value.textContent);
            if (index > 0) {
                ids += ', ';
            }
            ids += facebookId;
        });
        return ids;
    };

    replaceFacebookIdsWithNames = function (members) {
        var members2 = [], key;
        for (key in members) {
            if (members.hasOwnProperty(key)) {
                members2.push(members[key]);
                $("#tabTable tr td:contains(" + members[key].id + ")").html(members[key].name);
            }
        }
    };

    playerDashboard.fetchAndDisplayFBNamesForInviteTable = function () {
        var requiredIds = extractRequiredFacebookUserIdsFromInviteTable();
        FB.api('?fields=name&ids=' + requiredIds, replaceFacebookIdsWithNames);
    };

}());

$(document).ready(function () {
    // Tooltip only Text
    var mousex, mousey;
    $('.masterTooltip').hover(function () {
        // Hover over code
        var title = $(this).attr('title');
        $(this).data('tipText', title).removeAttr('title');
        $('<p class="tooltip"></p>')
            .text(title)
            .appendTo('body')
            .fadeIn('slow');
    }, function () {
        // Hover out code
        $(this).attr('title', $(this).data('tipText'));
        $('.tooltip').remove();
    }).mousemove(function (e) {
        mousex = e.pageX + 20;
        mousey = e.pageY + 10;
        $('.tooltip')
            .css({ top: mousey, left: mousex });
    });
});

(function ($) {
    $.fn.extend({
        confirmAction: function () {
            var widget = $(this);

            widget.click(function () {
                return confirm($(this).attr('data-message'));
            });
        },
        disableButtonOnFormSubmission: function () {
            var widget = $(this);

            widget.submit(function () {
                $(this).find('input[type=submit]').attr('disabled', 'disabled');
                return true;
            });
        },
        tabbedPane: function () {
            var widget = $(this),
                header = widget.find('.tabs-header'),
                body = widget.find('.tab-body');

            header.find('.tab').click(function () {
                var tabName = $(this).attr('data-tabname');
                header.find('.tab').removeClass('selected');
                body.find('.tab-content').removeClass('selected');

                body.find('.activationAware').trigger('activated');
                header.find('.tab[data-tabname=' + tabName + ']').addClass('selected');
                body.find('.tab-content[data-tabname=' + tabName + ']').addClass('selected');
            });
        },
        statusHistory: function () {
            var widget = $(this),
                spinner = widget.find('.spinner'),
                contentDiv = widget.find('.content'),
                playerIdField = widget.find('[name=playerId]'),
                showMessage = function (message) {
                    spinner.hide(200);
                    contentDiv.val(message);
                    contentDiv.show(200);
                };

            contentDiv.hide();

            widget.addClass('activationAware');
            widget.on('activated', function () {
                var playerId = playerIdField.val();
                contentDiv.hide(200);
                spinner.show(200);
                $.ajax(YAZINO.config.baseUrl + 'player/' + playerId + '/statusHistory')
                    .done(function (result) {
                        var tbody,
                            auditDate;
                        contentDiv.empty().append('<table class="report"><thead><tr><th>Date</th><th>Changed By</th><th>From</th><th>To</th><th>Reason</th></tr></thead><tbody></tbody></table>');
                        tbody = contentDiv.find('tbody');
                        for (i = 0; i < result.length; ++i) {
                            auditDate = new Date(result[i].timestamp);
                            tbody.append($('<tr></tr>')
                                .append($('<td></td>').html($.datepicker.formatDate('yy-mm-dd', auditDate)
                                    + ' ' + auditDate.toTimeString()))
                                .append($('<td></td>').html(result[i].changedBy))
                                .append($('<td></td>').html(result[i].oldStatus))
                                .append($('<td></td>').html(result[i].newStatus))
                                .append($('<td></td>').html(result[i].reason)));
                        }
                        contentDiv.show(200);
                        spinner.hide(200);
                    })
                    .fail(function (jqXHR) {
                        var result = JSON.parse(jqXHR.responseText);
                        contentDiv.empty();
                        showMessage(result.message);
                    });
            });
        },
        disputeManager: function () {
            var widget = $(this),
                resolveButton = widget.find('.resolveDispute'),
                dialogue = widget.find('.resolveDialogue'),
                dialogueCancel = widget.find('.resolveDialogue .cancel'),
                dialogueSubmit = widget.find('.resolveDialogue .submit'),
                form = widget.find('.resolveDialogue form'),
                transactionIdDisplay = widget.find('.transactionId'),
                transactionIdField = form.find('[name=internalTransactionId]');

            dialogueCancel.click(function () {
                dialogue.hide(200);
                return false;
            });

            dialogueSubmit.click(function () {
                dialogue.hide(200);
                return true;
            });

            resolveButton.click(function () {
                var showPosition = resolveButton.position(),
                    transactionId = $(this).attr('data-transaction-id');
                dialogue.css('top', showPosition.top + resolveButton.height() + 4);
                dialogue.css('left', showPosition.left - dialogue.width() - 16 + resolveButton.width());

                form.attr('action', YAZINO.config.baseUrl + 'payments/disputes/resolve/' + transactionId);
                transactionIdDisplay.html(transactionId);
                transactionIdField.val(transactionId);

                dialogue.show(200);
            });
        },
        playerBlocker: function (newStatus) {
            var widget = $(this),
                showButton = widget.find('.performBlock'),
                playerIdField = widget.find('[name=playerId]'),
                dialogue = widget.find('.blockDialogue'),
                reasonField = dialogue.find('[name=blockReason]'),
                submitButton = dialogue.find('.submitBlock'),
                cancelButton = dialogue.find('.cancel'),
                messageField = dialogue.find('.message'),
                showMessage = function (text) {
                    messageField.text(text);
                    messageField.addClass('warn');
                    messageField.show();
                };

            dialogue.prepend('<div class="upArrow"></div>');

            showButton.click(function () {
                var showPosition = showButton.position();
                dialogue.css('top', showPosition.top + showButton.height() + 4);
                dialogue.css('left', showPosition.left);

                if (newStatus === "BLOCKED") {
                    submitButton.text("Block");
                } else {
                    submitButton.text("Unblock");
                }

                dialogue.show(200);
            });

            cancelButton.click(function () {
                dialogue.hide(200);
            });

            submitButton.click(function () {
                var reasonText = reasonField.val(),
                    playerId = playerIdField.val();
                if (reasonText === null || reasonText.length === 0) {
                    showMessage('A reason is required');
                    return;
                }
                submitButton.attr('disabled', 'disabled');
                $.ajax(YAZINO.config.baseUrl + 'player/' + playerId + '/changeStatusTo/' + newStatus, {
                    data: { reason: reasonText }
                })
                    .done(function (result) {
                        location.href = YAZINO.config.baseUrl + 'player/' + playerId;
                    })
                    .fail(function (jqXHR) {
                        var result = JSON.parse(jqXHR.responseText);
                        showMessage(result.message);
                    })
                    .always(function () {
                        submitButton.removeAttr('disabled');
                    });

            });
        },
        playerCloser: function () {
            var widget = $(this),
                showButton = widget.find('.performClose'),
                playerIdField = widget.find('[name=playerId]'),
                dialogue = widget.find('.closeDialogue'),
                reasonField = dialogue.find('[name=closeReason]'),
                submitButton = dialogue.find('.submitClose'),
                cancelButton = dialogue.find('.cancel'),
                messageField = dialogue.find('.message'),
                showMessage = function (text) {
                    messageField.text(text);
                    messageField.addClass('warn');
                    messageField.show();
                };

            dialogue.prepend('<div class="upArrow"></div>');

            showButton.click(function () {
                var showPosition = showButton.position();
                dialogue.css('top', showPosition.top + showButton.height() + 4);
                dialogue.css('left', showPosition.left);
                dialogue.show(200);
            });

            cancelButton.click(function () {
                dialogue.hide(200);
            });

            submitButton.click(function () {
                var reasonText = reasonField.val(),
                    playerId = playerIdField.val();
                if (reasonText === null || reasonText.length === 0) {
                    showMessage('A reason is required');
                    return;
                }
                if (confirm('This cannot be undone.\n\nClose player ' + playerId + '?')) {
                    submitButton.attr('disabled', 'disabled');
                    $.ajax(YAZINO.config.baseUrl + 'player/' + playerId + '/changeStatusTo/CLOSED', {
                        data: { reason: reasonText }
                    })
                        .done(function (result) {
                            location.reload();
                        })
                        .fail(function (jqXHR) {
                            var result = JSON.parse(jqXHR.responseText);
                            showMessage(result.message);
                        })
                        .always(function () {
                            submitButton.removeAttr('disabled');
                        });
                }
            });
        },
        playerBalanceAdjuster: function () {
            var widget = $(this),
                amountField = widget.find('[name=adjustmentAmount]'),
                playerIdField = widget.find('[name=playerId]'),
                adjustButton = widget.find('.performAdjustment'),
                balanceField = widget.find('.balance-value'),
                messageField = widget.find('.message'),
                addCommas = function (number) {
                    var splitString = String(number).split('.'),
                        wholePart = splitString[0],
                        decimalPart = splitString.length > 1 ? '.' + splitString[1] : '',
                        spacingRegex = /(\d+)(\d{3})/;
                    while (spacingRegex.test(wholePart)) {
                        wholePart = wholePart.replace(spacingRegex, '$1' + ',' + '$2');
                    }
                    return wholePart + decimalPart;
                },
                showMessage = function (text, error) {
                    messageField.text(text);
                    if (error) {
                        messageField.addClass('warn');
                    } else {
                        messageField.removeClass('warn');
                    }
                    messageField.show();
                };

            adjustButton.attr('disabled', 'disabled');

            if (!String.prototype.trim) {
                String.prototype.trim = function () {
                    return this.replace(/^\s+|\s+$/g, '');
                };
            }

            amountField.keydown(function (event) {
                var allowedKeyCodes = [8, 9, 13, 27, 46, 109, 189, 190];
                if ($.inArray(event.keyCode, allowedKeyCodes) !== -1 ||
                        (event.keyCode === 65 && event.ctrlKey === true) ||
                        (event.keyCode >= 35 && event.keyCode <= 39)) {
                    return;

                } else if (event.shiftKey || ((event.keyCode < 48 || event.keyCode > 57) && (event.keyCode < 96 || event.keyCode > 105))) {
                    event.preventDefault();
                }
            });
            amountField.keyup(function () {
                if (amountField.val().trim().length > 0) {
                    adjustButton.removeAttr('disabled');
                } else {
                    adjustButton.attr('disabled', 'disabled');
                }
            });

            adjustButton.click(function () {
                var amount = amountField.val(),
                    numberAmount = parseFloat(amount, 10),
                    playerId = playerIdField.val();
                if (isNaN(numberAmount)) {
                    showMessage(amount + ' is not a number', true);
                } else {
                    adjustButton.attr('disabled', 'disabled');
                    $.ajax(YAZINO.config.baseUrl + 'player/' + playerId + '/adjust/' + numberAmount)
                        .done(function (result) {
                            balanceField.text(addCommas(result.balance.toFixed(2)));
                            showMessage(result.message, false);
                        })
                        .fail(function (jqXHR) {
                            var result = JSON.parse(jqXHR.responseText);
                            showMessage(result.message, true);
                        })
                        .always(function () {
                            adjustButton.removeAttr('disabled');
                        });
                }
            });
        },
        formRefiner: function () {
            var widget = $(this),
                refinerAttr = 'data-form-refiner',
                refineeAttr = 'data-form-refinee',
                refiners = widget.find('input[' + refinerAttr + ']');

            console.log('refiner set up with [%d] refiners', refiners.length);

            function updateAreas() {
                var refinerName = $(this).attr(refinerAttr),
                    refinerPath = '*[' + refineeAttr + '="' + refinerName + '"]',
                    refineeAreas = widget.find(refinerPath);

                console.log('refining for [%s] with path [%s] and found [%d] elems', refinerName, refinerPath, refineeAreas.length);
                if ($(this).is(':checked')) {
                    refineeAreas.show();
                } else {
                    refineeAreas.hide();
                }
            }

            refiners.click(updateAreas).each(updateAreas);
        },
        promotionTypeWidget: function () {
            var widget = $(this),
                chooserAttr = 'promo-type-chooser',
                buyChips = 'buy-chips-form',
                dailyAward = 'daily-award-form',
                gifting = 'gifting-form',
                choosers = widget.find('input[' + chooserAttr + ']');

            console.log('chooser set up with [%d] op', choosers.length);

            function updateAreas() {

                var buyChipsWidget = widget.find('*[' + chooserAttr + '="buyChips"]'),
                    dailyAwardWidget = widget.find('*[' + chooserAttr + '="dailyAward"]'),
                    giftingWidget = widget.find('*[' + chooserAttr + '="gifting"]'),
                    value = $("input[name=promotionType]:checked").val();

                buyChipsWidget.hide();
                dailyAwardWidget.hide();
                giftingWidget.hide();
                if (value === "BUY_CHIPS") {
                    buyChipsWidget.show();
                } else if (value === "DAILY_AWARD") {
                    dailyAwardWidget.show();
                } else if (value === "GIFTING") {
                    giftingWidget.show();
                }
            }

            choosers.click(updateAreas).each(updateAreas);

        },
        chipPackageEditorWidget: function () {
            return this.each(function () {
                var widget = $(this),
                    elements = {
                        configArea: widget.find('.multiplierConfig'),
                        chipQuantityEditors: widget.find('.chipQuantityEditor'),
                        form: {
                            multiplier: $('<input/>'),
                            multiplierLabel: $('<label/>').text('Multiplier (%)'),
                            applyToAll: $('<button type="button"/>').text('apply multiplier'),
                            resetAll: $('<button type="button"/>').text('reset default values')
                        }
                    };

                function resetFieldToPercentageOfDefault(field, percentage) {
                    var defaultValue = parseInt(field.attr('data-default-value'), 10),
                        adjustedValue = defaultValue * percentage / 100;
                    field.val(adjustedValue);
                }

                function resetChipQuantityEditorToDefaultValue() {
                    resetFieldToPercentageOfDefault($(this).find('input'), 100);
                }

                elements.configArea
                    .append(elements.form.multiplierLabel
                        .append(elements.form.multiplier)
                        .append(elements.form.applyToAll))
                    .append(elements.form.resetAll);

                elements.chipQuantityEditors.each(function () {
                    var that = this;
                    $(this).find('button').click(function () {
                        resetChipQuantityEditorToDefaultValue.apply(that);
                    });
                });

                elements.form.resetAll.click(function () {
                    elements.chipQuantityEditors.each(resetChipQuantityEditorToDefaultValue);
                    return false;
                });

                function applyMultiplierToAll() {
                    var multiplier = parseInt(elements.form.multiplier.val(), 10);
                    if (isNaN(multiplier)) {
                        return;
                    }
                    elements.chipQuantityEditors.each(function () {
                        resetFieldToPercentageOfDefault($(this).find('input'), multiplier);
                    });
                }

                elements.form.applyToAll.click(function () {
                    applyMultiplierToAll();
                    return false;
                });

                elements.form.multiplier.keydown(function (e) {
                    if (e.keyCode === 13) {
                        applyMultiplierToAll();
                        return false;
                    }
                });

            });
        },
        promotionSearchFormWidget: function () {
            return this.each(function () {
                var widget = $(this),
                    elements = {
                        promotionTypeSelector: widget.find('#promotionType'),
                        createButton: widget.find('.create')
                    };

                function resetFieldToPercentageOfDefault(field, percentage) {
                    var defaultValue = parseInt(field.attr('data-default-value'), 10),
                        adjustedValue = defaultValue * percentage / 100;
                    field.val(adjustedValue);
                }

                function onPromotionTypeChanged() {
                    var promotionType = elements.promotionTypeSelector.val();

                    if (promotionType === 'BUY_CHIPS' || promotionType === 'DAILY_AWARD') {
                        elements.createButton.show();
                    } else {
                        elements.createButton.hide();
                    }

                }

                elements.promotionTypeSelector.change(onPromotionTypeChanged);
            });
        }
    });

    $(document).ready(function () {
        $('body').formRefiner();
        $('body').promotionTypeWidget();
        $('form.dashboard').disableButtonOnFormSubmission();
        $('.tabbed-pane').tabbedPane();
        $('.chipPackageEditor').chipPackageEditorWidget();
        $('.promotionSearchForm').promotionSearchFormWidget();
        $('.playerBalanceAdjuster').playerBalanceAdjuster();
        $('.disputeManager').disputeManager();
        $('.confirmAction').confirmAction();
        $('.fromDatePickable').each(function (index) {
            $(this).datepicker({ dateFormat: 'yy-mm-dd', defaultDate: -1 });
        });
        $('.statusHistory').statusHistory();
        $('.toDatePickable').each(function (index) {
            $(this).datepicker({ dateFormat: 'yy-mm-dd', defaultDate: -1 });
        });
        $('.fromDateTimePickable').each(function (index) {
            $(this).datetimepicker({dateFormat: 'yy-mm-dd', timeFormat: 'HH:mm'});
        });
        $('.toDateTimePickable').each(function (index) {
            $(this).datetimepicker({dateFormat: 'yy-mm-dd', timeFormat: 'HH:mm'});
        });
        $('#campaign.channels-4').change(function () {
            $('#evDiv').toggle();
            console.log('toggle evDiv');
        });

        // Chrome 25 and above support datetime-local natively and clash with jQuery-UI
        chromeVersion = window.navigator.appVersion.match(/Chrome\/(\d+)\./);
        if (chromeVersion === null || parseInt(chromeVersion[1], 10) < 25) {
            jQuery(".dateTimePicker[readonly!=readonly]").datetimepicker({
                showSecond: true,
                dateFormat: 'yy-mm-dd',
                timeFormat: 'HH:mm',
                separator: 'T'
            });
            jQuery(".datePicker[readonly!=readonly]").datepicker({
                dateFormat: 'yy-mm-dd'
            });
        }

        $('.formattedReport').submit(function () {
            var form = $(this),
                format = form.find('select[name=reportFormat]').val();
            if (format && format.length > 0) {
                form.attr('action', form.attr('data-action') + '.' + format);
            } else {
                form.attr('action', form.attr('data-action'));
            }
        });
    });
}(jQuery));
