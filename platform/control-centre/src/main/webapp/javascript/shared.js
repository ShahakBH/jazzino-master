/*global window, document, confirm, jQuery */
(function ($) {
    jQuery(document).ready(function () {
        var reindexNestedRows = function (tableBody) {
                var index = 0;
                tableBody.children('tr').each(function () {
                    jQuery(this).find('input,select').each(function () {
                        var input = jQuery(this),
                            name = input.attr('name'),
                            id = input.attr('id');
                        input.attr('name', name.replace(/\[\d+\]/g, '[' + index + ']'));
                        input.attr('id', id.replace(/\[\d+\]/g, '[' + index + ']'));
                    });
                    index += 1;
                });
            },
            scrollTo = function (element) {
                $('html, body').animate({ scrollTop: element.offset().top }, 200);
            },
            chromeVersion;

        jQuery(".action-delete").each(function () {
            jQuery(this).click(function () {
                var link = jQuery(this);
                return confirm('Delete ' + link.attr('data-type')
                    + ' ' + link.attr('data-id') + '?');
            });
        });
        jQuery(".action-named").each(function () {
            jQuery(this).click(function () {
                var link = jQuery(this),
                    dataType = link.attr('data-type') !== null ? ' ' + link.attr('data-type') : '',
                    dataId = link.attr('data-id') !== null ? ' ' + link.attr('data-id') : '';

                return confirm(link.attr('data-action') + dataType + dataId + '?');
            });
        });
        jQuery(".form-url-location").each(function () {
            var form = jQuery(this),
                url = form.attr('action'),
                fieldName = form.attr('data-field');
            form.submit(function () {
                jQuery.each(form.serializeArray(), function (i, field) {
                    if (field.name === fieldName) {
                        form.filter('input[type=submit]').attr('disabled', 'disabled');
                        window.location.href = url.replace('@', field.value);
                    }
                });
                return false;
            });
        });
        jQuery(".form-url-submit").each(function () {
            var form = jQuery(this),
                url = form.attr('action'),
                fieldName = form.attr('data-field');
            form.submit(function () {
                jQuery.each(form.serializeArray(), function (i, field) {
                    if (field.name === fieldName) {
                        form.attr('action', url.replace('@', field.value));
                    }
                });
                form.filter('input[type=submit]').attr('disabled', 'disabled');
                return true;
            });
        });
        jQuery("a.submit").each(function () {
            var link = jQuery(this);
            link.click(function () {
                link.parent('form').submit();
            });
        });
        jQuery("input.valuetype-number").keydown(function (event) {
            if (event.keyCode !== 46 && event.keyCode !== 8 && event.keyCode !== 190 && event.keyCode !== 9) {
                if ((event.keyCode < 48 || event.keyCode > 57) && (event.keyCode < 96 || event.keyCode > 105)) {
                    event.preventDefault();
                }
            }
        });

        jQuery(".filter").change(function () {
            var combo = jQuery(this),
                url = combo.attr('data-url'),
                wildcard = combo.attr('data-wildcard'),
                value = combo.val();
            if (value === wildcard) {
                window.location.href = combo.attr('data-wildcard-url');
            } else {
                window.location.href = url.replace('@', value);
            }
        });

        jQuery("pre.collapsed, pre.expanded").attr('title', 'Click to toggle view');
        jQuery("pre.collapsed, pre.expanded").click(function () {
            var element = jQuery(this);
            element.toggleClass("collapsed");
            element.toggleClass("expanded");
        });

        jQuery(".pointer").change(function () {
            jQuery('.' + this.id).val(this.value);
        });

        jQuery(".thumbnail").mouseenter(function () {
            jQuery(this).find('div').fadeIn(100);
        });
        jQuery(".thumbnail").mouseleave(function () {
            jQuery(this).find('div').fadeOut(100);
        });

        jQuery("input.populateImage").change(function () {
            var input = jQuery(this),
                imageName = input.attr('value'),
                destination = input.siblings('img.populateImage'),
                sourceFormat = destination.attr('data-srcformat');
            destination.attr('src', (sourceFormat + '').replace('%IMAGE%', imageName));
        });

        jQuery("a.removeRow").click(function () {
            var link = jQuery(this),
                row = link.parents('tr'),
                tableBody = row.parents('tbody');
            row.detach();
            reindexNestedRows(tableBody);
        });
        jQuery("a.addRow").click(function () {
            var link = jQuery(this),
                tableBody = jQuery('#' + link.attr('data-tableId')).find('tbody'),
                template = jQuery('#' + link.attr('data-templateId')),
                newRow = template.clone(true);
            newRow.attr('id', null);
            tableBody.append(newRow);
            reindexNestedRows(tableBody);
            scrollTo(newRow);
            newRow.find('input').first().focus();
        });

        // Chrome 25 and above support datetime-local natively and clash with jQuery-UI
        chromeVersion = window.navigator.appVersion.match(/Chrome\/(\d+)\./);
        if (chromeVersion === null || parseInt(chromeVersion[1], 10) < 25) {
            jQuery(".dateTimePicker[readonly!=readonly]").datetimepicker({
                showSecond: true,
                dateFormat: 'yy-mm-dd',
                timeFormat: 'hh:mm',
                separator: 'T'
            });
            jQuery(".datePicker[readonly!=readonly]").datepicker({
                dateFormat: 'yy-mm-dd'
            });
        }

        jQuery('.default-focus').first().focus();
    });
}(jQuery));
