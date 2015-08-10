package strata.server.worker.audit.playedtracking.consumerexample;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LaunchExample {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:/strata/server/worker/audit/playedtracking/consumerexample/context.xml");
    }
}
