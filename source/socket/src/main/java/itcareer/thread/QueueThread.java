package itcareer.thread;

import itcareer.cmd.Command;
import itcareer.cmd.ResponseCode;
import itcareer.common.json.Devices;
import itcareer.common.json.Message;
import itcareer.constant.VideoConstant;
import itcareer.handler.MyChannelWSGroup;
import itcareer.model.ClientChannel;
import itcareer.model.TranscodeConfig;
import itcareer.model.event.NotificationEvent;
import itcareer.model.push.PushNotiRequest;
import itcareer.model.request.ProcessVideoRequest;
import itcareer.model.response.DoneVideoProcessResponse;
import itcareer.queue.RabbitMqSingleton;
import itcareer.utils.FFmpegUtils;
import itcareer.utils.QueueService;
import itcareer.utils.SocketService;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Getter
public class QueueThread extends AbstractRunable {
    private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(QueueThread.class);
    private String data;

    public QueueThread(String data) {
        this.data = data;
    }

    public void run() {
        try {
            LOG.debug("BACKEND CALL =====> {}", data);
            Message message = Message.fromJson(data, Message.class);
            if (message != null) {

                switch (message.getApp()) {
                    case Devices.BACKEND_APP:
                        handleBackendApp(message);
                        break;
                    default:
                        LOG.info("NO sub command process with: " + message.getSubCmd());
                }
            } else {
                LOG.error("message null or channel id null");
            }
        }catch (Exception e){
            LOG.error(e.getMessage(), e);
        }
    }

    private void handleBackendApp(Message message){
        switch (message.getCmd()) {
            case Command.BACKEND_POST_NOTIFICATION:
                handlePostNoti(message);
                break;
            case Command.BACKEND_PROCESS_VIDEO:
                handleProcessVideo(message);
                break;
            default:
                LOG.info("NO sub command process with: " + message.getSubCmd());
        }
    }

    /**
     *
     * handlePostNoti
     *{
     * 	"cmd": "BACKEND_PROCESS_VIDEO",
     * 	"app": "BACKEND_APP",
     * 	"data": {
     * 		"url": "path-to-file",
     * 	    "simulationId":"4567",
     * 	    "subTaskId": 1234,
     * 	    "tsSecond": 10,
     * 	    "cutStart": "00:00:00",
     * 	    "cutEnd": "00:10:00",
     * 	    "thumbnail": "00:09:00"
     *   }
     * }
     * */
    private void handleProcessVideo(Message message) {
        ProcessVideoRequest data = message.getDataObject(ProcessVideoRequest.class);
        LOG.info("==========> data of msg: {}", data);

        TranscodeConfig config = getConfig(data);

        String rootPath = QueueService.getInstance().getStringResource("file.upload-dir");

        // Get folder of video
        Path fileFullPath = FileSystems.getDefault().getPath(data.getUrl());
        Path folderPath = fileFullPath.getParent();
        String folderPathString = folderPath.toString();

        String inputPath = rootPath + VideoConstant.DIRECTORY_GENERAL + fileFullPath;
        String outputPath = rootPath + VideoConstant.DIRECTORY_GENERAL + folderPathString + File.separator + data.getSimulationId() + File.separator + data.getSubTaskId();
        LOG.info("=============> Output Folder Path: {}", outputPath);

        String thumbnailPath = String.join(File.separator, folderPathString, data.getSimulationId().toString(),data.getSubTaskId().toString(), "poster.jpg");

        String newVideoPath = String.join(File.separator, folderPathString, data.getSimulationId().toString(),data.getSubTaskId().toString(), "livestream.m3u8");
        try {
            long duration = FFmpegUtils.handleConvertToM3U8(inputPath, outputPath, config);
            sendMsgToQueue(true, data.getSimulationId(), data.getSubTaskId(), thumbnailPath, newVideoPath, duration);
        } catch (IOException | InterruptedException e) {
            LOG.info("============> PROCESS VIDEO FAILED WITH ERROR: {}", e.getMessage());
            sendMsgToQueue(false, data.getSimulationId(),data.getSubTaskId(),null, null, 0);
        }
    }
    private void sendMsgToQueue(boolean isSuccess, Long simulationId,Long subTaskId, String thumbnail, String newVideoPath, long duration) {
        DoneVideoProcessResponse data = new DoneVideoProcessResponse();
        data.setThumbnail(thumbnail);
        data.setSimulationId(simulationId);
        data.setSubTaskId(subTaskId);
        data.setIsSuccess(isSuccess);
        data.setContentPath(newVideoPath);
        data.setVideoDuration(duration);

        Message message = new Message();
        message.setApp(QueueService.getInstance().getStringResource("queue.msg.app"));
        message.setCmd(Command.MEDIA_COMPLETED_PROCESS_VIDEO);
        message.setData(data);

        LOG.info("========> SEND MSG: {} \n TO QUEUE: {}", message.toJson(), QueueService.getInstance().getStringResource("queue.video.pushback"));
        RabbitMqSingleton.getInstance().sendMessage(message.toJson(),QueueService.getInstance().getStringResource("queue.video.pushback"));
    }

    private TranscodeConfig getConfig(ProcessVideoRequest data) {
        TranscodeConfig config = new TranscodeConfig();
        config.setTsSeconds(data.getTsSecond() == null ? QueueService.getInstance().getStringResource("video.ts.second.default") : data.getTsSecond());
        return config;
    }

    /**
     *
     * handlePostNoti
     *{
     * 	"cmd": "BACKEND_POST_NOTIFICATION",
     * 	"app": "BACKEND_APP",
     * 	"data": {
     * 		"kind": 1,
     * 		"app": "ELMS",
     * 		"message": "Noi dung msg here",
     * 		"userId": 1234,
     * 		"cmd": "BACKEND_POST_NOTIFICATION"
     *   }
     * }
     * */
    private void handlePostNoti(Message message){
        NotificationEvent notificationEvent = message.getDataObject(NotificationEvent.class);
        if(notificationEvent != null && notificationEvent.getUserId()!=null){
            ClientChannel clientChannel = SocketService.getInstance().getClientChannel(notificationEvent.getUserId().toString());
            if(clientChannel != null){
                PushNotiRequest pushNotiRequest = new PushNotiRequest();
                pushNotiRequest.setApp(notificationEvent.getApp());
                pushNotiRequest.setMessage(notificationEvent.getMessage());

                Message messagePost = new Message();
                messagePost.setCmd(Command.CLIENT_RECEIVED_PUSH_NOTIFICATION);
                messagePost.setApp(Devices.BACKEND_SOCKET_APP);
                messagePost.setResponseCode(ResponseCode.RESPONSE_CODE_SUCCESS);
                messagePost.setData(pushNotiRequest);

                MyChannelWSGroup.getInstance().sendMessage(clientChannel.getChannelId(),message.toJson());
            }else{
                LOG.info("Not found user: {}", notificationEvent.getUserId());
            }
        }
    }

}
