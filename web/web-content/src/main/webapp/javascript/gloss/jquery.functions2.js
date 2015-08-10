//TODO: Refactor custom elements of this into yazino.js and export placeholder.js to its own file.

jQuery(document).ready(function() {
	$('#how-to-play #grey-window-body>div>h4').click(function(){
	  if ($(this).hasClass('active')){$(this).removeClass('active');
	  $(this).next().slideUp(1000);}
	  else {$(this).addClass('active');
	  $(this).next().slideDown(1000);}
 });                     

 $('#number-of-players input:checked').parent('.radio').addClass('checked');
 $('#number-of-players .radio').click(function(){
   $(this).addClass('checked');
   $(this).siblings().removeClass('checked');
 });                                                  



 if (($('#level-bar-filler').width() / $('#level-bar').width()) > 0.16) {
   $('.currently').css('color', '#fff');
 } else {
     $('.currently').css('color', '#a1a1a1');
 }

 if (($('#level-bar-filler').width() / $('#level-bar').width())  > 0.91) {
   $('.max-level').css('color', '#fff');
 }       
           
 $('#techno-div #tabs li').click(function(){
   var gametype;
   if ($('#games-area').hasClass('BLACKJACK')) gametype = 'BlackJack';
   if ($('#games-area').hasClass('TEXAS_HOLDEM')) gametype = 'Poker';
   if ($('#games-area').hasClass('ROULETTE')) gametype = 'roulette';
   $(this).siblings().removeClass('current');
   $(this).addClass('current');
   var assetUrl = YAZINO.configuration.get('contentUrl');
   if (assetUrl.match('/$') === null) {
      assetUrl += '/';
   }
   if ($(this).hasClass('game-tab')) {
    $('#option-area #tab_game').css('display', 'block');
    $('#option-area #tab_game').siblings().css('display', 'none');
   }
   if ($(this).hasClass('tournament-tab')) {
    $('#option-area #tab_tournament').css('display', 'block');
    $('#option-area #tab_tournament').siblings().css('display', 'none');
   }
   if ($(this).hasClass('my-table-tab')) {
    $('#option-area #tab_my_table').css('display', 'block');
    $('#option-area #tab_my_table').siblings().css('display', 'none');
   }
 });

});

/**
 * jQuery Placeholder 2.2.1 - jQuery plugin
 *
 * Copyright (c) 2010 Pavel Virskiy - me@pashinblog.ru http://pashinblog.ru
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * To enable, simply set attribute "placeholder" for default value.
 */
$(function() {

    // don't implement over the top of existing implimentations, adapted from http://diveintohtml5.info/detect.html
    var tmpInput = document.createElement('input');
    if ('placeholder' in tmpInput) {
        return;
    }

//	$('<style>').html('.jqueryPlaceholder{color:#a9a9a9!important;}').appendTo('head');
	$('input[placeholder]').each(function() {
		if ($(this).val() == '' || $(this).val() == $(this).attr('placeholder')) {
			$(this).addClass('jqueryPlaceholder').val($(this).attr('placeholder'))
		}
		$(this).focus(function() {
			if ($(this).val() == $(this).attr('placeholder')) {
				$(this).removeClass('jqueryPlaceholder').val('')
			}
		});
		$(this).blur(function() {
			if ($(this).val() == '') {
				$(this).addClass('jqueryPlaceholder').val($(this).attr('placeholder'))
			}
		})
	});
	$('textarea[placeholder]').each(function() {
		if (this.value == '' || this.value == $(this).attr('placeholder')) {
			$(this).addClass('jqueryPlaceholder');
			this.value = $(this).attr('placeholder')
		}
		;
		$(this).focus(function() {
			if (this.value == $(this).attr('placeholder')) {
				$(this).removeClass('jqueryPlaceholder');
				this.value = ''
			}
		});
		$(this).blur(function() {
			if (this.value == '') {
				$(this).addClass('jqueryPlaceholder');
				this.value = $(this).attr('placeholder')
			}
		})
	});
	$('form').each(function() {
		$(this).submit(function() {
			$(this).find("input").each(function() {
				if ($(this).val() == $(this).attr('placeholder'))$(this).val('')
			});
			$(this).find("textarea").each(function() {
				if (this.value == $(this).attr('placeholder'))this.value = ''
			})
		})
	})
});
