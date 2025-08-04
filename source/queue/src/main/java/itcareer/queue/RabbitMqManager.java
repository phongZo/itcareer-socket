package itcareer.queue;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RabbitMqManager implements ShutdownListener {

    protected final static Logger logger = LogManager.getLogger(RabbitMqManager.class);
    protected final ConnectionFactory factory;
    protected final ScheduledExecutorService executor;
    protected volatile Connection connection = null;
    private static Channel channel = null;
    private final List<QueueListener> queueListeners = new CopyOnWriteArrayList<>();
    private Address[] address;

    public RabbitMqManager(final ConnectionFactory factory) {
        this.factory = factory;
        executor = Executors.newSingleThreadScheduledExecutor();
        connection = null;
    }

    public void setAddress(Address[] address) {
        this.address = address;
    }

    public Address[] getAddress() {
        return address;
    }

    public void addQueueListener(QueueListener listener) {
        this.queueListeners.add(listener);
    }
    public void start() {
        try {
            if (address != null) {
                connection = factory.newConnection(address);
            } else {
                connection = factory.newConnection();
            }

            connection.addShutdownListener(this);
            logger.info("Connected to {}:{}", factory.getHost(), factory.getPort());

            for (QueueListener listener : queueListeners) {
                if (listener != null && listener.getQueue() != null) {
                    doMyBusiness(listener);
                }
            }
        } catch (final Exception e) {
            logger.error("Failed to connect to {}:{}", factory.getHost(), factory.getPort(), e);
            asyncWaitAndReconnect();
        }
    }


    private void doMyBusiness(QueueListener listener) {
        String queueName = listener.getQueue();
        queueDeclare(queueName);

        try {
            Channel localChannel = connection.createChannel();
            Consumer consumer = new DefaultConsumer(localChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    try {
                        String message = new String(body, "UTF-8");
                        //logger.info(" [x] Received '{}' from queue '{}'", message, queueName);
                        listener.consumer(message, localChannel, deliveryTag);
                    } catch (Exception ex) {
                        logger.error("Error in queue '{}': {}", queueName, ex.getMessage(), ex);
                        localChannel.basicNack(deliveryTag, false, true);
                    }
                }
            };

            localChannel.basicConsume(queueName, false, consumer);
        } catch (Exception ex) {
            logger.error("Failed to consume queue '{}': {}", queueName, ex.getMessage(), ex);
        }
    }

    public void shutdownCompleted(final ShutdownSignalException cause) {
        // reconnect only on unexpected errors
        if (!cause.isInitiatedByApplication()) {
            logger.info("Lost connection to " + factory.getHost() + ":" + factory.getPort(),
                    cause);
            channel = null;
            connection = null;
            asyncWaitAndReconnect();
        }
    }

    protected void asyncWaitAndReconnect() {
        executor.schedule(new Runnable() {
            public void run() {
                start();
            }
        }, 15, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();

        if (connection == null) {
            return;
        }

        try {
            if (channel != null) {
                channel.close();
            }
            connection.close();
        } catch (final Exception e) {
            logger.error("Failed to close connection", e);
        } finally {
            connection = null;
        }
    }

    public Channel createChannel() {
        try {
            return connection == null ? null : connection.createChannel();
        } catch (final Exception e) {
            logger.error("Failed to create channel", e);
            return null;
        }
    }

    public void closeChannel(final Channel channel) {
        // isOpen is not fully trustable!
        if ((channel == null) || (!channel.isOpen())) {
            return;
        }

        try {
            channel.close();
        } catch (final Exception e) {
            logger.error("Failed to close channel: " + channel, e);
        }
    }

    public <T> T call(final ChannelCallable<T> callable) {
        final Channel channel = createChannel();

        if (channel != null) {
            try {
                return callable.call(channel);
            } catch (final Exception e) {
                logger.error("Failed to run: " + callable.getDescription() + " on channel: "
                        + channel, e);
            } finally {
                closeChannel(channel);
            }
        }

        return null;
    }

    private Channel getChannel() {
        if (channel == null || !channel.isOpen()) {
            channel = createChannel();
        }
        return channel;
    }

    public void queueDeclare(String queueName) {
        try {
            AMQP.Queue.DeclareOk ok = getChannel().queueDeclare(queueName, true, false, false, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void sendMessage(String message, String queueName) {
        try {
            getChannel().basicPublish("", queueName, null, message.getBytes("UTF-8"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

        }

    }
}
