package edu.vanderbilt.isis.meta.cdb;

import com.google.protobuf.TextFormat;
import edu.vanderbilt.isis.meta.CdbMsg;
import io.netty.buffer.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;


// @Sharable
public class ClientDesignMsgHandler extends ChannelInboundMessageHandlerAdapter<CdbMsg.Message> {
    private static final Logger logger = LoggerFactory
            .getLogger(AssemblyDesignBridgeClient.class);

    final CdbMsg.Message message;

    public ClientDesignMsgHandler(final CdbMsg.Message message) {
        this.message = message;
    }

    @Override
    public void messageReceived(ChannelHandlerContext channelHandlerContext, CdbMsg.Message in) throws Exception {
        logger.info("message received {}",in);
    }

    /**
     * This method is called when there is a problem.
     * The Throwable is logged and the Channel is closed, which means the connection to the server is closed.
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.warn("exception caught", cause);
        ctx.close();
    }

    /**
     * Called once the connection to the Servier is established.
     * Register with the distributor.
     * This method will be called once the connection is established.
     * Once the connection is established, a sequence of bytes is promptly sent to the Server.
     * Overriding this method ensures that something is written to the server as soon as possible.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("generating sample message {}", this.message);
        ctx.write(this.message);
    }

    /**
     * Called once the connection to the server is dropped.
     * Unregister with the distributor.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.warn("channel closed {}", ctx);
    }

}