package edu.vanderbilt.isis.meta.cdb;

import edu.vanderbilt.isis.meta.CdbMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import java.net.InetSocketAddress;

public class AssemblyDesignBridgeServer {
    private static final Logger logger = LoggerFactory
            .getLogger(AssemblyDesignBridgeServer.class);
    private final int port;

    public AssemblyDesignBridgeServer(final int port) {
        this.port = port;
    }

    public AssemblyDesignBridgeServer() {
        this(15150);
    }

    public static void main(String[] args) throws Exception {


        final CommandLineParser parser = new BasicParser();
        final Options options = new Options();

        options.addOption(OptionBuilder
                .withLongOpt("show-help")
                .withDescription("show the usage information")
                .hasArg(false)
                .create('h'));

        options.addOption(OptionBuilder
                .withLongOpt("show-log-config")
                .hasArg(false)
                .withDescription("show the logger configuration information")
                .create('L'));

        options.addOption(OptionBuilder
                .withLongOpt("load-log-config")
                .hasArg(true)
                .withArgName("LOAD-LOG-CONFIG")
                .withDescription("the path to the logback configuration file")
                .create('l'));

        options.addOption(OptionBuilder
                .withLongOpt("port")
                .withDescription("the port on the target host")
                .hasArg(true)
                .withArgName("PORT")
                .withType(Integer.TYPE)
                .create('P'));

        try {
            final CommandLine line = parser.parse(options, args);

            // validate that block-size has been set
            if (line.hasOption("show-log-config")) {
                final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                StatusPrinter.print(lc);
            }

            if (line.hasOption("show-help")) {
                usage(options);
            }

            final int port;
            if (!(line.hasOption("port"))) {
                port = 15150;
            } else {
                final String portStr = line.getOptionValue("port");
                port = Integer.parseInt(portStr);
            }
            new AssemblyDesignBridgeServer(port).start();

        } catch (ParseException ex) {
            logger.error("Unexpected exception", ex);
            usage(options);
        }
        logger.info("starting server");
        // print internal state
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);

        if (args.length != 1) {
            logger.error("Usage: {} <port>", AssemblyDesignBridgeServer.class.getSimpleName());
            new AssemblyDesignBridgeServer().start();
            return;
        }
        final int port = Integer.parseInt(args[0]);

    }

    private static void usage(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( AssemblyDesignBridgeServer.class.getSimpleName(), options );
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
                    pipe.addLast("frameDecoder", new MagicLengthFrameDecoder());
                    pipe.addLast("frameEncoder", new MagicLengthFrameEncoder());

                    pipe.addLast("protobufDecoder",
                            new ProtobufDecoder(CdbMsg.Control.getDefaultInstance()));
                    pipe.addLast("protobufEncoder", new ProtobufEncoder());

                    pipe.addLast("distributor", new DesignMsgHandler());
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
                    future.channel().localAddress());
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
