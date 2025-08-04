package itcareer;

/**
 * Created by mac on 5/22/16.
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import itcareer.queue.RabbitMqSingleton;
import itcareer.thread.CleanupClientChannel;
import itcareer.utils.QueueService;
import itcareer.utils.SnowFlakeIdService;
import itcareer.utils.SocketService;

import java.util.TimeZone;

public final class Server {
    public static final int PORT = Integer.valueOf(QueueService.getInstance().getStringResource("server.ws.port"));

    public static void main(String[] args) throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));   // It will set UTC timezone

        SocketService.getInstance().getTimer().scheduleAtFixedRate(new CleanupClientChannel(), 10000, 60000);

        //init snow flake
        SnowFlakeIdService.getInstance().setDataCenterId(0);
        SnowFlakeIdService.getInstance().setNodeId(0);

        //start queue
        RabbitMqSingleton.getInstance().startQueue();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebSocketServerInitializer());

            Channel ch = b.bind(PORT).sync().channel();

            System.out.println("Websocket server started successfully Open your web browser and navigate to http://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
