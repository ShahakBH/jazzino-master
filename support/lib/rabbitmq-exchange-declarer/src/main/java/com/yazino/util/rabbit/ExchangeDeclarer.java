package com.yazino.util.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Collections;

public class ExchangeDeclarer {
    private static final int RABBITMQ_PORT = 5672;

    private final String host;
    private final String virtualHost;
    private final String userName;
    private final String password;

    public ExchangeDeclarer(final String host, final String virtualHost, final String userName, final String password) {
        this.host = host;
        this.virtualHost = virtualHost;
        this.userName = userName;
        this.password = password;
    }

    public void createExchange(final String exchangeName, final String exchangeType, final boolean durable, final boolean autoDelete)
            throws Exception {
        final Connection connection = connect();
        Channel channel = connection.createChannel();
        try {
            channel.exchangeDeclarePassive(exchangeName);
        } catch (IOException e) {
            // exception means the channel is now useless, cannot even call close()
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, exchangeType, durable, autoDelete, Collections.<String, Object>emptyMap());
        }
        close(connection, channel);
    }

    private Connection connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setRequestedHeartbeat(0);
        factory.setVirtualHost(virtualHost);
        return factory.newConnection();
    }

    private void close(final Connection connection, final Channel channel) {
        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.out.println(String.format("Usage: java %s <rabbit-host> <virtual-host> <username> <password> <exchange-name> "
                + "[type=direct] [durable=true|false] [autodelete=true|false]",
                ExchangeDeclarer.class.getName()));
        System.exit(-1);
    }

    public static void main(final String[] args) {
        if (args.length < 5) {
            usage();
        }
        boolean durable = false;
        boolean autoDelete = false;
        String exchangeType = "direct";

        final String host = args[0];
        final String virtualHost = args[1];
        final String userName = args[2];
        final String password = args[3];
        final String exchangeName = args[4];
        if (args.length >= 6) {
            exchangeType = args[5];
        }
        if (args.length >= 7) {
            durable = Boolean.parseBoolean(args[6]);
        }
        if (args.length >= 8) {
            autoDelete = Boolean.parseBoolean(args[7]);
        }

        try {
            new ExchangeDeclarer(host, virtualHost, userName, password).createExchange(exchangeName, exchangeType, durable, autoDelete);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
