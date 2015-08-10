update PROMOTION_CONFIG_ARCHIVE set config_value = if(instr(config_value, 'images/gloss/'), substring_index(config_value,'/',-3), config_value)
where CONFIG_KEY in ('main.image', 'secondary.image', 'news.image')#
