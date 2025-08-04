package itcareer.thread;

import io.netty.channel.ChannelHandlerContext;
import itcareer.cmd.ResponseCode;
import itcareer.common.json.Message;
import itcareer.handler.MyChannelWSGroup;
import itcareer.jwt.UserSession;
import itcareer.model.ClientChannel;
import itcareer.model.response.ClientInfoResponse;
import itcareer.utils.SocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler {
    private static final Logger LOG = LogManager.getLogger(ClientHandler.class);
    private static ClientHandler instance = null;

    private ClientHandler(){

    }

    public static ClientHandler getInstance(){
        if(instance == null){
            instance = new ClientHandler();
        }
        return instance;
    }

    private boolean isValidSession(UserSession userSession){
        return userSession != null && userSession.getId() != null;
    }
    private void sendErrorMsg(ChannelHandlerContext channelHandlerContext, Message oldRequest, String msg){
        Message response = new Message();
        response.setCmd(oldRequest.getCmd());
        response.setMsg(msg);
        response.setResponseCode(ResponseCode.RESPONSE_CODE_ERROR);
        MyChannelWSGroup.getInstance().sendMessage(channelHandlerContext.channel(),response.toJson());
    }

    public void handlePing(ChannelHandlerContext channelHandlerContext, Message message){
        UserSession userSession = UserSession.fromToken(message.getToken());
        if(isValidSession(userSession)){
            handleCacheClientSession(userSession, channelHandlerContext);

            message.setData(new ClientInfoResponse());
            message.setToken(null);
            message.setMsg("Ping success: "+ userSession.getId());
            message.setResponseCode(ResponseCode.RESPONSE_CODE_SUCCESS);
            MyChannelWSGroup.getInstance().sendMessage(channelHandlerContext.channel(),message.toJson());
        }else{
            sendErrorMsg(channelHandlerContext, message, "Token invalid");
        }
    }
    public void handleClientInfo(ChannelHandlerContext channelHandlerContext, Message message){
        UserSession userSession = UserSession.fromToken(message.getToken());
        if(isValidSession(userSession)){
            handleCacheClientSession(userSession, channelHandlerContext);

            message.setData(new ClientInfoResponse());
            message.setToken(null);
            message.setMsg("Client info success: "+ userSession.getId());
            message.setResponseCode(ResponseCode.RESPONSE_CODE_SUCCESS);
            MyChannelWSGroup.getInstance().sendMessage(channelHandlerContext.channel(),message.toJson());
        }else{
            sendErrorMsg(channelHandlerContext, message, "Token invalid");
        }
    }
    private void handleCacheClientSession(UserSession userSession, ChannelHandlerContext channelHandlerContext){
        ClientChannel channel = SocketService.getInstance().getClientChannel(userSession.getId().toString());
        if(channel != null){
            //update old channel
            channel.setTime(System.currentTimeMillis());
            channel.setChannelId(MyChannelWSGroup.getInstance().getIdChannel(channelHandlerContext.channel()));
        }else{
            ClientChannel clientChannel = new ClientChannel();
            clientChannel.setChannelId(MyChannelWSGroup.getInstance().getIdChannel(channelHandlerContext.channel()));
            clientChannel.setTime(System.currentTimeMillis());
            SocketService.getInstance().addClientChannel(userSession.getId().toString(), clientChannel);
        }
    }
}
