ssh mem-prd-dbdw01.mem.yazino.com

mysql -uroot

use damjan

drop table if exists damjan.tmp_alyssa;

create table damjan.tmp_alyssa as
select
    player_id,
    if (
        max(audit_date) > curdate() - interval 14 day and max(audit_date) <= curdate() - interval 7 day,
        '7-13',
        if (
            max(audit_date) > curdate() - interval 21 day and max(audit_date) <= curdate() - interval 14 day,
            '14-20',
            if (
                max(audit_date) >= curdate() - interval 90 day and max(audit_date) < curdate() - interval 21 day,
                '21-90',
                'other'
            )
        )
    ) band
from strataproddw.rpt_account_activity
where audit_date >= curdate() - interval 90 day
group by 1;

alter table damjan.tmp_alyssa add primary key (player_id);

exit

mysql -uroot -e "select player_id from damjan.tmp_alyssa where band = '7-13'" > 7_13.csv
mysql -uroot -e "select player_id from damjan.tmp_alyssa where band = '14-20'" > 14_20.csv
mysql -uroot -e "select player_id from damjan.tmp_alyssa where band = '21-90'" > 21_90.csv

zip /tmp/promo.zip 7_13.csv 14_20.csv 21_90.csv

exit

scp mem-prd-dbdw01.mem.yazino.com:/tmp/promo.zip .
