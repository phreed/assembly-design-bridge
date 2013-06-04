package edu.vanderbilt.isis.meta.cdb;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import edu.vanderbilt.isis.meta.cdb.DesignFrameDecoder;
import edu.vanderbilt.isis.meta.cdb.DesignFrameEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vanderbilt.isis.meta.CdbMsg;

public class AssemblyDesignBridgeServer {
	private static final Logger logger = LoggerFactory
			.getLogger(AssemblyDesignBridgeServer.class);

    private final int port;

    public AssemblyDesignBridgeServer( final int port ) {
        this.port = port;
    }
    public AssemblyDesignBridgeServer() {
        this(15150);
    }

    public void start() throws Exception {
        final ServerBootstrap boot = new ServerBootstrap();
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    logger.trace("initialize socket");
                    final ChannelPipeline pipe = channel.pipeline();
                    pipe.addLast("frameDecoder", new DesignFrameDecoder());
                    pipe.addLast("protobufDecoder",
                            new ProtobufDecoder(CdbMsg.Message.getDefaultInstance()));

                    pipe.addLast("distributor",	new DesignMsgHandler());

                    pipe.addLast("frameEncoder",	new DesignFrameEncoder());
                    pipe.addLast("protobufEncoder", new ProtobufEncoder());
                }
            };

            boot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            final InetSocketAddress isoc = new InetSocketAddress(this.port);

            logger.info("creating the server socket {}:{}", isoc.getAddress(), this.port);
            final ChannelFuture future = boot.bind(isoc).sync();
            logger.info("{} started and listening on {}", AssemblyDesignBridgeServer.class.getCanonicalName(),
                    future.channel().localAddress()) ;
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

	public static void main(String[] args) throws Exception {
		logger.info("starting server");
		if (args.length != 1) {
            logger.error("Usage: {} <port>", AssemblyDesignBridgeServer.class.getSimpleName());
            new AssemblyDesignBridgeServer().start();
            return;
        }
        final int port = Integer.parseInt(args[0]);
        new AssemblyDesignBridgeServer(port).start();
	}
}
