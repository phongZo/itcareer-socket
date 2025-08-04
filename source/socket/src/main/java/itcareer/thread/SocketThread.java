package itcareer.thread;

import io.netty.channel.ChannelHandlerContext;
import itcareer.cmd.Command;
import itcareer.cmd.ResponseCode;
import itcareer.common.json.Devices;
import itcareer.common.json.Message;
import itcareer.handler.MyChannelWSGroup;
import lombok.Data;
import org.apache.logging.log4j.Logger;

@Data
public class SocketThread extends AbstractRunable {
    private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(SocketThread.class);
    private String msg;
    private ChannelHandlerContext channelHandlerContext;

    public SocketThread(String message, ChannelHandlerContext channelHandlerContext) {
        this.msg = message;
        this.channelHandlerContext = channelHandlerContext;
    }

    @Override
    public void run() {
        //gui vao queue de schedule lai
        Message message = Message.fromJson(msg, Message.class);
        //LOG.debug("msg: "+msg);
        if (message != null) {
            if(message.getToken() == null && !Command.ignoreToken(message.getCmd())){
                sendUnauthMsg(message);
                LOG.debug("Token require...");
            }else{
                switch (message.getApp()){
                    case Devices.CLIENT_APP:
                        handleClientApp(message);
                        break;
                    default:
                        sendErrorMsg(message);
                        break;
                }
            }
        } else {
            LOG.debug("data error " + msg + " is not json format");
        }
    }

    private void handleClientApp(Message message){
        switch (message.getCmd()){
            case Command.CLIENT_INFO:
                ClientHandler.getInstance().handleClientInfo(channelHandlerContext, message);
                break;
            case Command.CLIENT_PING:
                ClientHandler.getInstance().handlePing(channelHandlerContext, message);
                break;
            default:
                sendErrorMsg(message);
                break;
        }
    }

    private void sendErrorMsg(Message oldRequest){
        Message response = new Message();
        response.setCmd(oldRequest.getCmd());
        response.setMsg("Data error");
        response.setResponseCode(ResponseCode.RESPONSE_CODE_ERROR);
        MyChannelWSGroup.getInstance().sendMessage(channelHandlerContext.channel(),response.toJson());
    }

    private void sendUnauthMsg(Message oldRequest){
        Message response = new Message();
        response.setCmd(oldRequest.getCmd());
        response.setMsg("Data error");
        response.setResponseCode(ResponseCode.RESPONSE_CODE_UN_AUTH);
        MyChannelWSGroup.getInstance().sendMessage(channelHandlerContext.channel(),response.toJson());
    }
}
