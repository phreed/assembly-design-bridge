package edu.vanderbilt.isis.meta.cdb;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
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

public class AssemblyDesignBridgeClient {
	private static final Logger logger = LoggerFactory
			.getLogger(AssemblyDesignBridgeClient.class);

    private final String host;
    private final int port;

    public AssemblyDesignBridgeClient( final String host, final int port ) {
        this.host = host;
        this.port = port;
    }

    public AssemblyDesignBridgeClient() {
        this("localhost",15150);
    }

    public void start() throws Exception {
        final Bootstrap boot = new Bootstrap();
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final InetSocketAddress address = new InetSocketAddress(this.host, this.port);

        try {
            final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    logger.trace("initialize socket");
                    final ChannelPipeline pipe = channel.pipeline();
                    pipe.addLast("frameDecoder", new DesignFrameDecoder());
                    pipe.addLast("protobufDecoder",
                            new ProtobufDecoder(CdbMsg.Message.getDefaultInstance()));

                    pipe.addLast("frameEncoder",	new DesignFrameEncoder());
                    pipe.addLast("protobufEncoder", new ProtobufEncoder());
                }
            };

            boot.group(bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .remoteAddress(address)
                    .handler(initializer);

            final ChannelFuture future = boot.connect().sync();
            logger.info("{} started and listening on {}", AssemblyDesignBridgeClient.class.getCanonicalName(),
                    future.channel().localAddress()) ;
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

	public static void main(String[] args) throws Exception {
		logger.info("starting edu.vanderbilt.isis.meta.cdb");
		if (args.length != 2) {
            logger.error("Usage: {} <port>", AssemblyDesignBridgeClient.class.getSimpleName());
            new AssemblyDesignBridgeClient().start();
            return;
        }
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        new AssemblyDesignBridgeClient(host, port).start();
	}
}
