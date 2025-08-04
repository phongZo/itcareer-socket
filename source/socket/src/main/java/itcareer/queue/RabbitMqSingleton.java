package itcareer.queue;

import com.rabbitmq.client.Channel;
import itcareer.thread.QueueThread;
import itcareer.utils.QueueService;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public class RabbitMqSingleton {
    Logger logger = org.apache.logging.log4j.LogManager.getLogger(getClass());
    private static RabbitMqSingleton instance;
    private  ConnectionFactory factory;
    private final RabbitMqManager rabbitMqManager;

    public static RabbitMqSingleton getInstance() {
        if (instance == null) {
            instance = new RabbitMqSingleton();
        }
        return instance;
    }

    private RabbitMqSingleton() {
        factory = new ConnectionFactory();
        factory.setUsername(QueueService.getInstance().getStringResource("queue.username"));
        factory.setPassword(QueueService.getInstance().getStringResource("queue.password"));
        factory.setVirtualHost(QueueService.getInstance().getStringResource("queue.virtualHost"));

        String[] ips = QueueService.getInstance().getStringArray("queue.ip");
        String[] ports = QueueService.getInstance().getStringArray("queue.port");

        Address[] addrArr = null;
        if(ips != null && ips.length> 0){
            addrArr = new Address[ips.length];
            for(int i=0; i< ips.length; i++){
                System.out.println("RabbitMq connect: "+ips[i]+":"+ports[i]);
                addrArr[i]=  new Address(ips[i], Integer.parseInt(ports[i]));
            }
        }
        // simulate dependency management creation and wiring
        rabbitMqManager = new RabbitMqManager(factory);
        rabbitMqManager.setAddress(addrArr);

    }

    public void startQueue() {
        rabbitMqManager.addQueueListener(createListener("queue.video.process"));
        rabbitMqManager.addQueueListener(createListener("queue.notification"));
        rabbitMqManager.start();
    }

    private QueueListener createListener(String queueKey) {
        return new QueueListener() {
            @Override
            public String getQueue() {
                return QueueService.getInstance().getStringResource(queueKey);
            }

            @Override
            public void consumer(String message, Channel channel, long deliveryTag) throws IOException {
                try {
                    new QueueThread(message).run();
                    channel.basicAck(deliveryTag, false);
                    logger.info("{} Ack successful for deliveryTag: {}", queueKey, deliveryTag);
                } catch (Exception e) {
                    logger.error("{} Error processing message", queueKey, e);
                    channel.basicNack(deliveryTag, false, true);
                }
            }
        };
    }


    public void stopQueue() {
        rabbitMqManager.stop();
    }

    public void queueDeclare(String queueName) {
        rabbitMqManager.queueDeclare(queueName);

    }
    public void sendMessage(String message, String queueName) {
        queueDeclare(queueName);
        rabbitMqManager.sendMessage(message, queueName);
    }
}
