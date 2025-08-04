package itcareer.queue;

import com.rabbitmq.client.Channel;

/**
 * Created by mac on 6/15/16.
 */
public interface QueueListener {

    String getQueue();

    void consumer(String message, Channel channel, long deliveryTag) throws Exception;

}

