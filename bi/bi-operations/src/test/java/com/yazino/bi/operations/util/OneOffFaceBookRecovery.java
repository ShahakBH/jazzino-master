package com.yazino.bi.operations.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class OneOffFaceBookRecovery {

    public static void main(String[] args) {
        if (args.length == 1) {
            DateTime startDate = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(args[0]);
            DateTime endDate = startDate.plusDays(1);

            System.out.println("getting facebook ad stats for: " + args[0]);
            ApplicationContext context =
                    new ClassPathXmlApplicationContext(
                            "/com/yazino/bi/operations/util/one-off-recovery.xml");

            RecoveryService service = context.getBean(RecoveryService.class);
            service.recover(startDate, endDate);
        } else {
            System.out.println("Usage: OneOffFaceBookRecovery date");
            System.out.println("where date is the day to recover facebook ad stats for. format is YYYY-MM-DD");
        }
    }
}
