package strata.server.performance.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.lang.reflect.Method;

public class QueueAdministrator {

    private final CachingConnectionFactory mFactory = new CachingConnectionFactory();
    private RabbitAdmin mRabbitAdmin;

    public void deleteQueue(String queueName) {
        setupRabbitAdmin();
        mRabbitAdmin.deleteQueue(queueName);
    }

    public void declareQueue(String queueName) {
        setupRabbitAdmin();
        mRabbitAdmin.declareQueue(new Queue(queueName));
    }

    private void setupRabbitAdmin() {
        if (mRabbitAdmin == null) {
            mRabbitAdmin = new RabbitAdmin(mFactory);
        }
    }

    public CachingConnectionFactory getConnectionFactory() {
        return mFactory;
    }

    public static void main(String[] args) throws Exception {
        String host = args[0];
        String virtualHost = args[1];
        String username = args[2];
        String password = args[3];
        String action = args[4];
        String actionArgument = args[5];

        System.out.println(String.format("Attempting to administer host [%s], virtualHost [%s], username [%s], password [%s]", host, virtualHost, username, password));

        QueueAdministrator administrator = new QueueAdministrator();
        administrator.getConnectionFactory().setVirtualHost(virtualHost);
        administrator.getConnectionFactory().setHost(host);
        administrator.getConnectionFactory().setPassword(password);
        administrator.getConnectionFactory().setUsername(username);
        administrator.getConnectionFactory().setPort(5672);

        Method method = administrator.getClass().getMethod(action, String.class);
        method.invoke(administrator, actionArgument);

        System.out.println(String.format("Performed %s(%s).", action, actionArgument));
        System.exit(0);
    }

}
