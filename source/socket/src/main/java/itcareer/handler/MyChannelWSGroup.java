package itcareer.handler;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import itcareer.utils.SocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mac on 5/21/16.
 */
public class MyChannelWSGroup {
    private final static Logger LOG = LogManager.getLogger(MyChannelWSGroup.class.getName());
    private static MyChannelWSGroup instance = null;
    private static final Object lock = new Object();
    private ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    private MyChannelWSGroup(){

    }

    public synchronized static MyChannelWSGroup getInstance() {
        if(instance == null){
            synchronized(lock) {
                instance = new MyChannelWSGroup();
            }
        }
        return instance;
    }
    public String getIdChannel(Channel channel){
        return SocketService.getInstance().getIdChannel(channel);
    }
    public void addChannel(Channel channel){
        channels.put(getIdChannel(channel),channel);
    }

    public void removeChannel(Channel channel){
        channels.remove(getIdChannel(channel));

    }

    public Long getCCU(){
        return channels.mappingCount();
    }

    public void sendMessage(String channelId, String message){

        Channel channel = channels.get(channelId);
        if(channel != null && channel.isActive()){
            try {
                LOG.info("[Socket] >>> Sending notification {}", message);
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }catch (Exception e){
                LOG.error(e.getMessage(),e);
            }

        }else{
            LOG.error("khong send duoc channel null");

        }
    }

    public boolean checkChannel(String channelId){
        return channels.containsKey(channelId);
    }

    public void sendMessage(Channel channel, String message){
        if(channel != null && channel.isActive()){
            try {
                LOG.info("[Socket] >>> Sending notification {}", message);
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }catch (Exception e){
                LOG.error(e.getMessage(),e);
            }

        }else{
            LOG.error("khong send duoc channel null");

        }
    }


    public void sendBroadcastMessage(String message){
        TextWebSocketFrame msg = new TextWebSocketFrame(message);
        for(Channel channel: channels.values()){
            try {
                if(channel != null && channel.isActive()) {
                    channel.writeAndFlush(msg);
                }
            }catch (Exception e){
                LOG.error(e.getMessage(),e);
            }
        }

    }
}
