CREATE TABLE  if not exists PROMOTION_CONFIG_ARCHIVE (
  PROMO_ID INT(11) NOT NULL,
  CONFIG_KEY VARCHAR(50) NOT NULL,
  CONFIG_VALUE VARCHAR(2000) NOT NULL,
  PRIMARY KEY (PROMO_ID, CONFIG_KEY),
  FOREIGN KEY FK_PROMOTION_CONFIG_ARCHIVE(PROMO_ID) REFERENCES PROMOTION_ARCHIVE (PROMO_ID)
) ENGINE = InnoDB#

DROP PROCEDURE IF EXISTS addToPromoConfig#
CREATE PROCEDURE addToPromoConfig()
BEGIN

    if (select count(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'strataproddw' and table_name = 'DAILY_AWARD_CONFIGURATION_ARCHIVE') = 1 then
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'main.image', main_image from DAILY_AWARD_CONFIGURATION_ARCHIVE where main_image is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'main.image.link', main_image_link from DAILY_AWARD_CONFIGURATION_ARCHIVE where main_image_link is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'secondary.image', secondary_image from DAILY_AWARD_CONFIGURATION_ARCHIVE where secondary_image is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'secondary.image.link', secondary_image_link from DAILY_AWARD_CONFIGURATION_ARCHIVE where secondary_image_link is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'news.image', news_image from DAILY_AWARD_CONFIGURATION_ARCHIVE where news_image is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'news.image.link', news_image_link from DAILY_AWARD_CONFIGURATION_ARCHIVE where news_image_link is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'news.header', news_header from DAILY_AWARD_CONFIGURATION_ARCHIVE where news_header is not null;
        replace into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
            select promo_id, 'news.text', news_text from DAILY_AWARD_CONFIGURATION_ARCHIVE where news_text is not null;
    end if;

	if (select count(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'strataproddw' and table_name = 'PROMOTION_ARCHIVE' and COLUMN_NAME = 'REWARD_CHIPS') = 1 then
      insert into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
          select promo_id, 'reward.chips', reward_chips from PROMOTION_ARCHIVE where reward_chips is not null;
      insert into PROMOTION_CONFIG_ARCHIVE(promo_id, config_key, config_value)
          select promo_id, 'max.rewards', max_rewards from PROMOTION_ARCHIVE where max_rewards is not null;

      alter table PROMOTION_ARCHIVE drop column REWARD_CHIPS;
      alter table PROMOTION_ARCHIVE drop column MAX_REWARDS;
	end if;
END#
call addToPromoConfig()#
DROP PROCEDURE IF EXISTS addToPromoConfig#

drop table if exists DAILY_AWARD_CONFIGURATION_ARCHIVE#