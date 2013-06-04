package edu.vanderbilt.isis.meta.cdb;

import edu.vanderbilt.isis.meta.CdbMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import java.net.InetSocketAddress;

public class AssemblyDesignBridgeClient {
    private static final Logger logger = LoggerFactory
            .getLogger(AssemblyDesignBridgeClient.class);
    private final String host;
    private final int port;

    public AssemblyDesignBridgeClient(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public AssemblyDesignBridgeClient() {
        this("localhost", 15150);
    }

    /**
     * This method handles the command line optionss.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        logger.info("starting edu.vanderbilt.isis.meta.cdb");

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
                .withLongOpt("host")
                .withDescription("the name or address of the target host")
                .hasArg()
                .withArgName("HOST")
                .create('H'));

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

            final String host;
            if (!(line.hasOption("host"))) {
                host = "localhost";
            } else {
                host = line.getOptionValue("host");
            }

            final int port;
            if (!(line.hasOption("port"))) {
                port = 15150;
            } else {
                final String portStr = line.getOptionValue("port");
                port = Integer.parseInt(portStr);
            }
            new AssemblyDesignBridgeClient(host, port).start();

        } catch (ParseException ex) {
              logger.error("Unexpected exception", ex);
            usage(options);
        }
    }

    private static void usage(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( AssemblyDesignBridgeClient.class.getSimpleName(), options );
    }

    /**
     * This method does the real work.
     *
     * @throws Exception
     */
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

                    pipe.addLast("reporter", new ClientDesignMsgHandler());

                    pipe.addLast("frameEncoder", new DesignFrameEncoder());
                    pipe.addLast("protobufEncoder", new ProtobufEncoder());
                }
            };

            boot.group(bossGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(address)
                    .handler(initializer);

            final ChannelFuture future = boot.connect().sync();
            logger.info("{} started and connected to {} on {}",
                    AssemblyDesignBridgeClient.class.getSimpleName(),
                    future.channel().remoteAddress(),
                    future.channel().localAddress());
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }
}
