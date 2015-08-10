package com.strata.consumer;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RabbitMQConsumer {

    private Thread thread;
    private final String[] routingKeys;
    private ConnectionFactory connectionFactory;

    private final ConcurrentLinkedQueue<QueueingConsumer.Delivery> deliveries = new ConcurrentLinkedQueue<QueueingConsumer.Delivery>();

    public RabbitMQConsumer(String host, String user, String password, String vhost, String... routingKeys) {
        this.routingKeys = routingKeys;
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setVirtualHost(vhost);
        connectionFactory.setUsername(user);
        connectionFactory.setPassword(password);
    }

    public void start() throws IOException {
        final Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();
        final AMQP.Queue.DeclareOk queue = channel.queueDeclare();
        for (String routingKey : routingKeys) {
            channel.queueBind(queue.getQueue(), "player-direct", routingKey);
        }
        thread = new Thread(new Runnable() {
            public void run() {
                boolean sendAcknowledgement = false;
                boolean interrupted = false;
                QueueingConsumer consumer = new QueueingConsumer(channel);
                try {
                    channel.basicConsume(queue.getQueue(), sendAcknowledgement, consumer);
                    while (!interrupted) {
                        try {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            deliveries.add(delivery);
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            interrupted = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    public void stop() {
        if (thread != null)
            thread.interrupt();
    }

    public QueueingConsumer.Delivery getNextDelivery(){
        return deliveries.remove();
    }

    public boolean isStopped() {
        return thread == null || thread.getState() == Thread.State.TERMINATED;
    }

}